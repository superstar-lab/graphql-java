package graphql.analysis;

import graphql.PublicApi;
import graphql.execution.ConditionalNodes;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.AstTransformer;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeTraverser.LeaveOrEnter;
import graphql.language.NodeUtil;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.language.TypeName;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.language.NodeTraverser.LeaveOrEnter.LEAVE;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;
import static java.util.Collections.singletonList;

/**
 * Helps to traverse (or reduce) a Document (or parts of it) and tracks at the same time the corresponding Schema types.
 * <p>
 * This is an important distinction to just traversing the Document without any type information: Each field has a clearly
 * defined type. See {@link QueryVisitorFieldEnvironment}.
 * <p>
 * Further are the built in Directives skip/include automatically evaluated: if parts of the Document should be ignored they will not
 * be visited. But this is not a full evaluation of a Query: every fragment will be visited/followed regardless of the type condition.
 *
 * It also doesn't consider field merging, which means for example {@code { user{firstName} user{firstName}} } will result in four
 * visitField calls.
 */
@PublicApi
public class QueryTraversal {

    private final Collection<? extends Node> roots;
    private final GraphQLSchema schema;
    private final Map<String, FragmentDefinition> fragmentsByName;
    private final Map<String, Object> variables;

    private final ConditionalNodes conditionalNodes = new ConditionalNodes();
    private final ValuesResolver valuesResolver = new ValuesResolver();
    private final GraphQLObjectType rootParentType;

    private QueryTraversal(GraphQLSchema schema,
                           Document document,
                           String operation,
                           Map<String, Object> variables) {
        assertNotNull(document, "document  can't be null");
        NodeUtil.GetOperationResult getOperationResult = NodeUtil.getOperation(document, operation);
        this.schema = assertNotNull(schema, "schema can't be null");
        this.variables = assertNotNull(variables, "variables can't be null");
        this.fragmentsByName = getOperationResult.fragmentsByName;
        this.roots = singletonList(getOperationResult.operationDefinition);
        this.rootParentType = getRootTypeFromOperation(getOperationResult.operationDefinition);
    }

    private QueryTraversal(GraphQLSchema schema,
                           Node root,
                           GraphQLObjectType rootParentType,
                           Map<String, FragmentDefinition> fragmentsByName,
                           Map<String, Object> variables) {
        this.schema = assertNotNull(schema, "schema can't be null");
        this.variables = assertNotNull(variables, "variables can't be null");
        assertNotNull(root, "root can't be null");
        this.roots = Collections.singleton(root);
        this.rootParentType = assertNotNull(rootParentType, "rootParentType can't be null");
        this.fragmentsByName = assertNotNull(fragmentsByName, "fragmentsByName can't be null");
    }

    public Object visitDepthFirst(QueryVisitor queryVisitor) {
        return visitImpl(queryVisitor, null);
    }

    /**
     * Visits the Document (or parts of it) in post-order.
     *
     * @param visitor the query visitor that will be called back
     */
    public void visitPostOrder(QueryVisitor visitor) {
        visitImpl(visitor, false);
    }

    /**
     * Visits the Document (or parts of it) in pre-order.
     *
     * @param visitor the query visitor that will be called back
     */
    public void visitPreOrder(QueryVisitor visitor) {
        visitImpl(visitor, true);
    }


    public Node transform(QueryVisitorWithControl queryVisitor) {
        NodeVisitorImpl nodeVisitor = new NodeVisitorImpl(queryVisitor, new QueryVisitorWithControlStub());
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(QueryTraversalContext.class, new QueryTraversalContext(rootParentType, rootParentType, null, null));
        //TODO: assert roots has only single element
        return new AstTransformer().transform(roots.stream().findFirst().get(), nodeVisitor, rootVars);
    }

    /**
     * Reduces the fields of a Document (or parts of it) to a single value. The fields are visited in post-order.
     *
     * @param queryReducer the query reducer
     * @param initialValue the initial value to pass to the reducer
     * @param <T>          the type of reduced value
     *
     * @return the calculated overall value
     */
    @SuppressWarnings("unchecked")
    public <T> T reducePostOrder(QueryReducer<T> queryReducer, T initialValue) {
        // compiler hack to make acc final and mutable :-)
        final Object[] acc = {initialValue};
        visitPostOrder(new QueryVisitorStub() {
            @Override
            public void visitField(QueryVisitorFieldEnvironment env) {
                acc[0] = queryReducer.reduceField(env, (T) acc[0]);
            }
        });
        return (T) acc[0];
    }

    /**
     * Reduces the fields of a Document (or parts of it) to a single value. The fields are visited in pre-order.
     *
     * @param queryReducer the query reducer
     * @param initialValue the initial value to pass to the reducer
     * @param <T>          the type of reduced value
     *
     * @return the calucalated overall value
     */
    @SuppressWarnings("unchecked")
    public <T> T reducePreOrder(QueryReducer<T> queryReducer, T initialValue) {
        // compiler hack to make acc final and mutable :-)
        final Object[] acc = {initialValue};
        visitPreOrder(new QueryVisitorStub() {
            @Override
            public void visitField(QueryVisitorFieldEnvironment env) {
                acc[0] = queryReducer.reduceField(env, (T) acc[0]);
            }
        });
        return (T) acc[0];
    }

    private GraphQLObjectType getRootTypeFromOperation(OperationDefinition operationDefinition) {
        switch (operationDefinition.getOperation()) {
            case MUTATION:
                return assertNotNull(schema.getMutationType());
            case QUERY:
                return assertNotNull(schema.getQueryType());
            case SUBSCRIPTION:
                return assertNotNull(schema.getSubscriptionType());
            default:
                return assertShouldNeverHappen();
        }
    }

    private List<Node> childrenOf(Node<?> node) {
        if (!(node instanceof FragmentSpread)) {
            return node.getChildren();
        }
        FragmentSpread fragmentSpread = (FragmentSpread) node;
        return singletonList(fragmentsByName.get(fragmentSpread.getName()));
    }

    private Object visitImpl(QueryVisitor visitFieldCallback, Boolean preOrder) {
        Map<Class<?>, Object> rootVars = new LinkedHashMap<>();
        rootVars.put(QueryTraversalContext.class, new QueryTraversalContext(rootParentType, rootParentType, null, null));

        QueryVisitor preOrderCallback;
        QueryVisitor postOrderCallback;
        if (preOrder == null) {
            preOrderCallback = visitFieldCallback;
            postOrderCallback = visitFieldCallback;
        } else {
            QueryVisitor noOp = new QueryVisitorStub();
            preOrderCallback = preOrder ? visitFieldCallback : noOp;
            postOrderCallback = !preOrder ? visitFieldCallback : noOp;
        }

        NodeTraverser nodeTraverser = new NodeTraverser(rootVars, this::childrenOf);
        NodeVisitorImpl visitor = new NodeVisitorImpl(new QueryVisitorAdapter(preOrderCallback), new QueryVisitorAdapter(postOrderCallback));
        return nodeTraverser.depthFirst(visitor, roots);
    }

    private class NodeVisitorImpl extends NodeVisitorStub {

        final QueryVisitorWithControl preOrderCallback;
        final QueryVisitorWithControl postOrderCallback;

        NodeVisitorImpl(QueryVisitorWithControl preOrderCallback, QueryVisitorWithControl postOrderCallback) {
            this.preOrderCallback = preOrderCallback;
            this.postOrderCallback = postOrderCallback;
        }

        @Override
        public TraversalControl visitInlineFragment(InlineFragment inlineFragment, TraverserContext<Node> context) {
            boolean shouldInclude = conditionalNodes.shouldInclude(variables, inlineFragment.getDirectives());


            QueryVisitorInlineFragmentEnvironment inlineFragmentEnvironment = new QueryVisitorInlineFragmentEnvironmentImpl(inlineFragment, shouldInclude, context);

            if (context.getVar(LeaveOrEnter.class) == LEAVE) {
                return postOrderCallback.visitInlineFragment(inlineFragmentEnvironment);
            }

            TraversalControl traversalControl = preOrderCallback.visitInlineFragment(inlineFragmentEnvironment);
            if(traversalControl != TraversalControl.CONTINUE) {
                return traversalControl;
            }
            // inline fragments are allowed not have type conditions, if so the parent type counts
            QueryTraversalContext parentEnv = context.getVarFromParents(QueryTraversalContext.class);

            GraphQLCompositeType fragmentCondition;
            if (inlineFragment.getTypeCondition() != null) {
                TypeName typeCondition = inlineFragment.getTypeCondition();
                fragmentCondition = (GraphQLCompositeType) schema.getType(typeCondition.getName());
            } else {
                fragmentCondition = parentEnv.getRawType();
            }
            // for unions we only have other fragments inside
            context.setVar(QueryTraversalContext.class, new QueryTraversalContext(fragmentCondition, fragmentCondition, parentEnv.getEnvironment(), inlineFragment));
            return traversalControl;
        }

        @Override
        public TraversalControl visitFragmentSpread(FragmentSpread fragmentSpread, TraverserContext<Node> context) {
            boolean shouldInclude = conditionalNodes.shouldInclude(variables, fragmentSpread.getDirectives());

            FragmentDefinition fragmentDefinition = fragmentsByName.get(fragmentSpread.getName());
            shouldInclude &= conditionalNodes.shouldInclude(variables, fragmentDefinition.getDirectives());

            QueryVisitorFragmentSpreadEnvironment fragmentSpreadEnvironment = new QueryVisitorFragmentSpreadEnvironmentImpl(fragmentSpread, fragmentDefinition,
                    shouldInclude, context);
            if (context.getVar(LeaveOrEnter.class) == LEAVE) {
                return postOrderCallback.visitFragmentSpread(fragmentSpreadEnvironment);
            }

            TraversalControl traversalControl = preOrderCallback.visitFragmentSpread(fragmentSpreadEnvironment);
            if(traversalControl != TraversalControl.CONTINUE) {
                return traversalControl;
            }

            QueryTraversalContext parentEnv = context.getVarFromParents(QueryTraversalContext.class);

            GraphQLCompositeType typeCondition = (GraphQLCompositeType) schema.getType(fragmentDefinition.getTypeCondition().getName());

            context
                    .setVar(QueryTraversalContext.class, new QueryTraversalContext(typeCondition, typeCondition, parentEnv.getEnvironment(), fragmentDefinition));
            return traversalControl;
        }

        @Override
        public TraversalControl visitField(Field field, TraverserContext<Node> context) {
            QueryTraversalContext parentEnv = context.getVarFromParents(QueryTraversalContext.class);

            GraphQLFieldDefinition fieldDefinition = Introspection.getFieldDef(schema, parentEnv.getRawType(), field.getName());
            boolean isTypeNameIntrospectionField = fieldDefinition == Introspection.TypeNameMetaFieldDef;
            GraphQLFieldsContainer fieldsContainer = !isTypeNameIntrospectionField ? (GraphQLFieldsContainer) unwrapAll(parentEnv.getOutputType()) : null;
            GraphQLCodeRegistry codeRegistry = schema.getCodeRegistry();
            Map<String, Object> argumentValues = valuesResolver.getArgumentValues(codeRegistry, fieldDefinition.getArguments(), field.getArguments(), variables);
            boolean shouldInclude = conditionalNodes.shouldInclude(variables, field.getDirectives());

            QueryVisitorFieldEnvironment environment = new QueryVisitorFieldEnvironmentImpl(isTypeNameIntrospectionField,
                    field,
                    fieldDefinition,
                    parentEnv.getOutputType(),
                    fieldsContainer,
                    parentEnv.getEnvironment(),
                    argumentValues,
                    parentEnv.getSelectionSetContainer(),
                    context,
                    shouldInclude);

            LeaveOrEnter leaveOrEnter = context.getVar(LeaveOrEnter.class);
            if (leaveOrEnter == LEAVE) {
                return postOrderCallback.visitField(environment);
            }

            TraversalControl traversalControl = preOrderCallback.visitField(environment);
            if(traversalControl != TraversalControl.CONTINUE) {
                return traversalControl;
            }

            GraphQLUnmodifiedType unmodifiedType = unwrapAll(fieldDefinition.getType());
            QueryTraversalContext fieldEnv = (unmodifiedType instanceof GraphQLCompositeType)
                    ? new QueryTraversalContext(fieldDefinition.getType(), (GraphQLCompositeType) unmodifiedType, environment, field)
                    : new QueryTraversalContext(null, null, environment, field);// Terminal (scalar) node, EMPTY FRAME


            context.setVar(QueryTraversalContext.class, fieldEnv);
            return traversalControl;
        }

    }

    public static Builder newQueryTraversal() {
        return new Builder();
    }

    @PublicApi
    public static class Builder {
        private GraphQLSchema schema;
        private Document document;
        private String operation;
        private Map<String, Object> variables;

        private Node root;
        private GraphQLObjectType rootParentType;
        private Map<String, FragmentDefinition> fragmentsByName;


        /**
         * The schema used to identify the types of the query.
         *
         * @param schema the schema to use
         *
         * @return this builder
         */
        public Builder schema(GraphQLSchema schema) {
            this.schema = schema;
            return this;
        }

        /**
         * specify the operation if a document is traversed and there
         * are more than one operation.
         *
         * @param operationName the operation name to use
         *
         * @return this builder
         */
        public Builder operationName(String operationName) {
            this.operation = operationName;
            return this;
        }

        /**
         * document to be used to traverse the whole query.
         * If set a {@link Builder#operationName(String)} might be required.
         *
         * @param document the document to use
         *
         * @return this builder
         */
        public Builder document(Document document) {
            this.document = document;
            return this;
        }

        /**
         * Variables used in the query.
         *
         * @param variables the variables to use
         *
         * @return this builder
         */
        public Builder variables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }

        /**
         * Specify the root node for the traversal. Needs to be provided if there is
         * no {@link Builder#document(Document)}.
         *
         * @param root the root node to use
         *
         * @return this builder
         */
        public Builder root(Node root) {
            this.root = root;
            return this;
        }

        /**
         * The type of the parent of the root node. (See {@link Builder#root(Node)}
         *
         * @param rootParentType the root parent type
         *
         * @return this builder
         */
        public Builder rootParentType(GraphQLObjectType rootParentType) {
            this.rootParentType = rootParentType;
            return this;
        }

        /**
         * Fragment by name map. Needs to be provided together with a {@link Builder#root(Node)} and {@link Builder#rootParentType(GraphQLObjectType)}
         *
         * @param fragmentsByName the map of fragments
         *
         * @return this builder
         */
        public Builder fragmentsByName(Map<String, FragmentDefinition> fragmentsByName) {
            this.fragmentsByName = fragmentsByName;
            return this;
        }

        /**
         * @return a built {@link graphql.analysis.QueryTraversal} object
         */
        public QueryTraversal build() {
            checkState();
            if (document != null) {
                return new QueryTraversal(schema, document, operation, variables);
            } else {
                return new QueryTraversal(schema, root, rootParentType, fragmentsByName, variables);
            }
        }

        private void checkState() {
            if (document != null || operation != null) {
                if (root != null || rootParentType != null || fragmentsByName != null) {
                    throw new IllegalStateException("ambiguous builder");
                }
            }
        }

    }
}
