package graphql.util;

import graphql.AssertException;
import graphql.Directives;
import graphql.PublicApi;
import graphql.Scalars;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitor;
import graphql.analysis.QueryVisitorFieldArgumentEnvironment;
import graphql.analysis.QueryVisitorFieldArgumentInputValue;
import graphql.analysis.QueryVisitorFieldArgumentValueEnvironment;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorFragmentSpreadEnvironment;
import graphql.analysis.QueryVisitorInlineFragmentEnvironment;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.Argument;
import graphql.language.AstPrinter;
import graphql.language.AstTransformer;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.Node;
import graphql.language.NodeVisitorStub;
import graphql.language.OperationDefinition;
import graphql.language.StringValue;
import graphql.language.TypeName;
import graphql.language.Value;
import graphql.language.VariableDefinition;
import graphql.language.VariableReference;
import graphql.parser.Parser;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLImplementingType;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLNamedOutputType;
import graphql.schema.GraphQLNamedSchemaElement;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.schema.GraphQLUnionType;
import graphql.schema.SchemaTransformer;
import graphql.schema.SchemaUtil;
import graphql.schema.TypeResolver;
import graphql.schema.idl.DirectiveInfo;
import graphql.schema.idl.ScalarInfo;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static graphql.Assert.assertNotNull;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.idl.SchemaGenerator.createdMockedSchema;
import static graphql.util.TraversalControl.CONTINUE;
import static graphql.util.TreeTransformerUtil.changeNode;

/**
 * Util class which converts schemas and optionally queries
 * into anonymized schemas and queries.
 */
@PublicApi
public class Anonymizer {

    public static class AnonymizeResult {
        private GraphQLSchema schema;
        private List<String> queries;

        public AnonymizeResult(GraphQLSchema schema, List<String> queries) {
            this.schema = schema;
            this.queries = queries;
        }

        public GraphQLSchema getSchema() {
            return schema;
        }

        public List<String> getQueries() {
            return queries;
        }
    }

    public static GraphQLSchema anonymizeSchema(String sdl) {
        return anonymizeSchemaAndQueries(createdMockedSchema(sdl), Collections.emptyList(), Collections.emptyMap()).schema;
    }

    public static GraphQLSchema anonymizeSchema(GraphQLSchema schema) {
        return anonymizeSchemaAndQueries(schema, Collections.emptyList(), Collections.emptyMap()).schema;
    }

    public static AnonymizeResult anonymizeSchemaAndQueries(String sdl, List<String> queries) {
        return anonymizeSchemaAndQueries(createdMockedSchema(sdl), queries, Collections.emptyMap());
    }

    public static AnonymizeResult anonymizeSchemaAndQueries(GraphQLSchema schema, List<String> queries) {
        return anonymizeSchemaAndQueries(schema, queries, Collections.emptyMap());
    }

    public static AnonymizeResult anonymizeSchemaAndQueries(String sdl, List<String> queries, Map<String, Object> variables) {
        return anonymizeSchemaAndQueries(createdMockedSchema(sdl), queries, variables);
    }

    public static AnonymizeResult anonymizeSchemaAndQueries(GraphQLSchema schema, List<String> queries, Map<String, Object> variables) {
        assertNotNull(queries, () -> "queries can't be null");

        AtomicInteger defaultStringValueCounter = new AtomicInteger(1);
        AtomicInteger defaultIntValueCounter = new AtomicInteger(1);

        Map<GraphQLNamedSchemaElement, String> newNameMap = recordNewNames(schema);

        SchemaTransformer schemaTransformer = new SchemaTransformer();
        GraphQLSchema newSchema = schemaTransformer.transform(schema, new GraphQLTypeVisitorStub() {

            @Override
            public TraversalControl visitGraphQLTypeReference(GraphQLTypeReference graphQLTypeReference, TraverserContext<GraphQLSchemaElement> context) {
                GraphQLNamedSchemaElement type = (GraphQLNamedSchemaElement) schema.getType(graphQLTypeReference.getName());
                String newName = newNameMap.get(type);
                GraphQLTypeReference newReference = GraphQLTypeReference.typeRef(newName);
                return changeNode(context, newReference);
            }

            @Override
            public TraversalControl visitGraphQLArgument(GraphQLArgument graphQLArgument, TraverserContext<GraphQLSchemaElement> context) {
                String newName = assertNotNull(newNameMap.get(graphQLArgument));
                GraphQLArgument newElement = graphQLArgument.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                    if (graphQLArgument.hasSetDefaultValue()) {
                        Value<?> defaultValueLiteral = ValuesResolver.valueToLiteral(graphQLArgument.getArgumentDefaultValue(), graphQLArgument.getType());
                        builder.defaultValueLiteral(replaceDefaultValue(defaultValueLiteral, defaultStringValueCounter, defaultIntValueCounter));
                    }
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType graphQLInterfaceType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLInterfaceType)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLInterfaceType));
                GraphQLInterfaceType newElement = graphQLInterfaceType.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                GraphQLCodeRegistry.Builder codeRegistry = assertNotNull(context.getVarFromParents(GraphQLCodeRegistry.Builder.class));
                TypeResolver typeResolver = codeRegistry.getTypeResolver(graphQLInterfaceType);
                codeRegistry.typeResolver(newName, typeResolver);
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLEnumType(GraphQLEnumType graphQLEnumType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLEnumType)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLEnumType));
                GraphQLEnumType newElement = graphQLEnumType.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLEnumValueDefinition(GraphQLEnumValueDefinition enumValueDefinition, TraverserContext<GraphQLSchemaElement> context) {
                String newName = assertNotNull(newNameMap.get(enumValueDefinition));
                GraphQLEnumValueDefinition newElement = enumValueDefinition.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition graphQLFieldDefinition, TraverserContext<GraphQLSchemaElement> context) {
                String newName = assertNotNull(newNameMap.get(graphQLFieldDefinition));
                GraphQLFieldDefinition newElement = graphQLFieldDefinition.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLDirective(GraphQLDirective graphQLDirective, TraverserContext<GraphQLSchemaElement> context) {
                if (Directives.DEPRECATED_DIRECTIVE_DEFINITION.getName().equals(graphQLDirective.getName())) {
                    GraphQLArgument reason = newArgument().name("reason")
                            .type(Scalars.GraphQLString)
                            .value(null).build();
                    GraphQLDirective newElement = graphQLDirective.transform(builder -> {
                        builder.description(null).argument(reason);
                    });
                    changeNode(context, newElement);
                    return TraversalControl.ABORT;
                }
                if (DirectiveInfo.isGraphqlSpecifiedDirective(graphQLDirective)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLDirective));
                GraphQLDirective newElement = graphQLDirective.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLInputObjectField(GraphQLInputObjectField graphQLInputObjectField, TraverserContext<GraphQLSchemaElement> context) {
                String newName = assertNotNull(newNameMap.get(graphQLInputObjectField));

                Value<?> defaultValue = null;
                if (graphQLInputObjectField.hasSetDefaultValue()) {
                    defaultValue = ValuesResolver.valueToLiteral(graphQLInputObjectField.getInputFieldDefaultValue(), graphQLInputObjectField.getType());
                    defaultValue = replaceDefaultValue(defaultValue, defaultStringValueCounter, defaultIntValueCounter);
                }

                Value<?> finalDefaultValue = defaultValue;
                GraphQLInputObjectField newElement = graphQLInputObjectField.transform(builder -> {
                    builder.name(newName);
                    if (finalDefaultValue != null) {
                        builder.defaultValueLiteral(finalDefaultValue);
                    }
                    builder.description(null);
                    builder.definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType graphQLInputObjectType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLInputObjectType)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLInputObjectType));
                GraphQLInputObjectType newElement = graphQLInputObjectType.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }


            @Override
            public TraversalControl visitGraphQLObjectType(GraphQLObjectType graphQLObjectType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLObjectType)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLObjectType));
                GraphQLObjectType newElement = graphQLObjectType.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLScalarType(GraphQLScalarType graphQLScalarType, TraverserContext<GraphQLSchemaElement> context) {
                if (ScalarInfo.isGraphqlSpecifiedScalar(graphQLScalarType)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLScalarType));
                GraphQLScalarType newElement = graphQLScalarType.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                return changeNode(context, newElement);
            }

            @Override
            public TraversalControl visitGraphQLUnionType(GraphQLUnionType graphQLUnionType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLUnionType)) {
                    return TraversalControl.ABORT;
                }
                String newName = assertNotNull(newNameMap.get(graphQLUnionType));
                GraphQLUnionType newElement = graphQLUnionType.transform(builder -> {
                    builder.name(newName).description(null).definition(null);
                });
                GraphQLCodeRegistry.Builder codeRegistry = assertNotNull(context.getVarFromParents(GraphQLCodeRegistry.Builder.class));
                TypeResolver typeResolver = codeRegistry.getTypeResolver(graphQLUnionType);
                codeRegistry.typeResolver(newName, typeResolver);
                return changeNode(context, newElement);
            }
        });

        List<String> newQueries = new ArrayList<>();
        for (String query : queries) {
            String newQuery = rewriteQuery(query, schema, newNameMap, variables);
            newQueries.add(newQuery);
        }
        AnonymizeResult result = new AnonymizeResult(newSchema, newQueries);
        return result;
    }

    private static Value replaceDefaultValue(Value defaultValueLiteral, AtomicInteger defaultStringValueCounter, AtomicInteger defaultIntValueCounter) {
        if (defaultValueLiteral instanceof StringValue) {
            return StringValue.newStringValue("defaultValue" + defaultStringValueCounter.getAndIncrement()).build();
        } else if (defaultValueLiteral instanceof IntValue) {
            return IntValue.newIntValue(BigInteger.valueOf(defaultIntValueCounter.getAndIncrement())).build();
        }
        return defaultValueLiteral;
    }

    public static Map<GraphQLNamedSchemaElement, String> recordNewNames(GraphQLSchema schema) {
        AtomicInteger objectCounter = new AtomicInteger(1);
        AtomicInteger inputObjectCounter = new AtomicInteger(1);
        AtomicInteger inputObjectFieldCounter = new AtomicInteger(1);
        AtomicInteger fieldCounter = new AtomicInteger(1);
        AtomicInteger scalarCounter = new AtomicInteger(1);
        AtomicInteger directiveCounter = new AtomicInteger(1);
        AtomicInteger argumentCounter = new AtomicInteger(1);
        AtomicInteger interfaceCounter = new AtomicInteger(1);
        AtomicInteger unionCounter = new AtomicInteger(1);
        AtomicInteger enumCounter = new AtomicInteger(1);
        AtomicInteger enumValueCounter = new AtomicInteger(1);

        Map<GraphQLNamedSchemaElement, String> newNameMap = new LinkedHashMap<>();

        Map<String, List<GraphQLImplementingType>> interfaceToImplementations =
                new SchemaUtil().groupImplementationsForInterfacesAndObjects(schema);

        GraphQLTypeVisitor visitor = new GraphQLTypeVisitorStub() {
            @Override
            public TraversalControl visitGraphQLArgument(GraphQLArgument graphQLArgument, TraverserContext<GraphQLSchemaElement> context) {
                String curName = graphQLArgument.getName();
                GraphQLSchemaElement parentNode = context.getParentNode();
                if (!(parentNode instanceof GraphQLFieldDefinition)) {
                    String newName = "argument" + argumentCounter.getAndIncrement();
                    newNameMap.put(graphQLArgument, newName);
                    return CONTINUE;
                }
                GraphQLFieldDefinition fieldDefinition = (GraphQLFieldDefinition) parentNode;
                String fieldName = fieldDefinition.getName();
                GraphQLImplementingType implementingType = (GraphQLImplementingType) context.getParentContext().getParentNode();
                Set<GraphQLFieldDefinition> matchingInterfaceFieldDefinitions = getSameFields(fieldName, implementingType.getName(), interfaceToImplementations, schema);
                String newName;
                if (matchingInterfaceFieldDefinitions.size() == 0) {
                    newName = "argument" + argumentCounter.getAndIncrement();
                } else {
                    List<GraphQLArgument> matchingArgumentDefinitions = getMatchingArgumentDefinitions(curName, matchingInterfaceFieldDefinitions);
                    if (matchingArgumentDefinitions.size() == 0) {
                        newName = "argument" + argumentCounter.getAndIncrement();
                    } else {
                        if (newNameMap.containsKey(matchingArgumentDefinitions.get(0))) {
                            newName = newNameMap.get(matchingArgumentDefinitions.get(0));
                        } else {
                            newName = "argument" + argumentCounter.getAndIncrement();
                            for (GraphQLArgument argument : matchingArgumentDefinitions) {
                                newNameMap.put(argument, newName);
                            }
                        }
                    }
                }
                newNameMap.put(graphQLArgument, newName);

                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType graphQLInterfaceType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLInterfaceType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Interface" + interfaceCounter.getAndIncrement();
                newNameMap.put(graphQLInterfaceType, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLEnumType(GraphQLEnumType graphQLEnumType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLEnumType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Enum" + enumCounter.getAndIncrement();
                newNameMap.put(graphQLEnumType, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLEnumValueDefinition(GraphQLEnumValueDefinition enumValueDefinition, TraverserContext<GraphQLSchemaElement> context) {
                String newName = "EnumValue" + enumValueCounter.getAndIncrement();
                newNameMap.put(enumValueDefinition, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition graphQLFieldDefinition, TraverserContext<GraphQLSchemaElement> context) {
                String fieldName = graphQLFieldDefinition.getName();
                GraphQLImplementingType parentNode = (GraphQLImplementingType) context.getParentNode();
                Set<GraphQLFieldDefinition> sameFields = getSameFields(fieldName, parentNode.getName(), interfaceToImplementations, schema);
                String newName;
                if (sameFields.size() == 0) {
                    newName = "field" + fieldCounter.getAndIncrement();
                } else {
                    if (newNameMap.containsKey(sameFields.iterator().next())) {
                        newName = newNameMap.get(sameFields.iterator().next());
                    } else {
                        newName = "field" + fieldCounter.getAndIncrement();
                        for (GraphQLFieldDefinition fieldDefinition : sameFields) {
                            newNameMap.put(fieldDefinition, newName);
                        }
                    }
                }
                newNameMap.put(graphQLFieldDefinition, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLDirective(GraphQLDirective graphQLDirective, TraverserContext<GraphQLSchemaElement> context) {
                if (DirectiveInfo.isGraphqlSpecifiedDirective(graphQLDirective)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Directive" + directiveCounter.getAndIncrement();
                newNameMap.put(graphQLDirective, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLInputObjectField(GraphQLInputObjectField graphQLInputObjectField, TraverserContext<GraphQLSchemaElement> context) {
                String newName = "inputField" + inputObjectFieldCounter.getAndIncrement();
                newNameMap.put(graphQLInputObjectField, newName);
                return CONTINUE;

            }

            @Override
            public TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType graphQLInputObjectType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLInputObjectType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "InputObject" + inputObjectCounter.getAndIncrement();
                newNameMap.put(graphQLInputObjectType, newName);
                return CONTINUE;
            }


            @Override
            public TraversalControl visitGraphQLObjectType(GraphQLObjectType graphQLObjectType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLObjectType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Object" + objectCounter.getAndIncrement();
                newNameMap.put(graphQLObjectType, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLScalarType(GraphQLScalarType graphQLScalarType, TraverserContext<GraphQLSchemaElement> context) {
                if (ScalarInfo.isGraphqlSpecifiedScalar(graphQLScalarType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Scalar" + scalarCounter.getAndIncrement();
                newNameMap.put(graphQLScalarType, newName);
                return CONTINUE;
            }

            @Override
            public TraversalControl visitGraphQLUnionType(GraphQLUnionType graphQLUnionType, TraverserContext<GraphQLSchemaElement> context) {
                if (Introspection.isIntrospectionTypes(graphQLUnionType)) {
                    return TraversalControl.ABORT;
                }
                String newName = "Union" + unionCounter.getAndIncrement();
                newNameMap.put(graphQLUnionType, newName);
                return CONTINUE;
            }
        };

        SchemaTransformer.transformSchema(schema, visitor);
        return newNameMap;
    }

    private static Set<GraphQLFieldDefinition> getSameFields(String fieldName,
                                                             String objectOrInterfaceName,
                                                             Map<String, List<GraphQLImplementingType>> interfaceToImplementations,
                                                             GraphQLSchema schema
    ) {
        Set<GraphQLFieldDefinition> result = new LinkedHashSet<>();
        Set<String> alreadyChecked = new LinkedHashSet<>();
        getSameFieldsImpl(fieldName, objectOrInterfaceName, interfaceToImplementations, schema, alreadyChecked, result);
        return result;
    }

    private static void getSameFieldsImpl(String fieldName,
                                          String curObjectOrInterface,
                                          Map<String, List<GraphQLImplementingType>> interfaceToImplementations,
                                          GraphQLSchema schema,
                                          Set<String> alreadyChecked,
                                          Set<GraphQLFieldDefinition> result) {
        if (alreadyChecked.contains(curObjectOrInterface)) {
            return;
        }
        alreadyChecked.add(curObjectOrInterface);

        // "up": get all Interfaces
        GraphQLImplementingType type = (GraphQLImplementingType) schema.getType(curObjectOrInterface);
        List<GraphQLNamedOutputType> interfaces = type.getInterfaces();
        getMatchingFieldDefinitions(fieldName, interfaces, result);
        for (GraphQLNamedOutputType interfaze : interfaces) {
            getSameFieldsImpl(fieldName, interfaze.getName(), interfaceToImplementations, schema, alreadyChecked, result);
        }

        // "down": get all Object or Interfaces
        List<GraphQLImplementingType> implementations = interfaceToImplementations.get(curObjectOrInterface);
        if (implementations == null) {
            return;
        }
        getMatchingFieldDefinitions(fieldName, implementations, result);
        for (GraphQLImplementingType implementingType : implementations) {
            getSameFieldsImpl(fieldName, implementingType.getName(), interfaceToImplementations, schema, alreadyChecked, result);
        }
    }

    private static void getMatchingFieldDefinitions(
            String fieldName,
            List<? extends GraphQLType> interfaces,
            Set<GraphQLFieldDefinition> result) {
        for (GraphQLType iface : interfaces) {
            GraphQLImplementingType implementingType = (GraphQLImplementingType) iface;
            if (implementingType.getFieldDefinition(fieldName) != null) {
                result.add(implementingType.getFieldDefinition(fieldName));
            }
        }
    }

    private static List<GraphQLArgument> getMatchingArgumentDefinitions(
            String name,
            Set<GraphQLFieldDefinition> fieldDefinitions) {
        List<GraphQLArgument> result = new ArrayList<>();
        for (GraphQLFieldDefinition fieldDefinition : fieldDefinitions) {
            Optional.ofNullable(fieldDefinition.getArgument(name)).map(result::add);
        }
        return result;
    }

    private static String rewriteQuery(String query, GraphQLSchema schema, Map<GraphQLNamedSchemaElement, String> newNames, Map<String, Object> variables) {
        AtomicInteger fragmentCounter = new AtomicInteger(1);
        AtomicInteger variableCounter = new AtomicInteger(1);
        Map<Node, String> nodeToNewName = new LinkedHashMap<>();
        Map<String, String> variableNames = new LinkedHashMap<>();
        Document document = new Parser().parseDocument(query);
        assertUniqueOperation(document);
        QueryTraverser queryTraverser = QueryTraverser.newQueryTraverser().document(document).schema(schema).variables(variables).build();
        queryTraverser.visitDepthFirst(new QueryVisitor() {

            @Override
            public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
                if (queryVisitorFieldEnvironment.isTypeNameIntrospectionField()) {
                    return;
                }
                String newName = assertNotNull(newNames.get(queryVisitorFieldEnvironment.getFieldDefinition()));
                nodeToNewName.put(queryVisitorFieldEnvironment.getField(), newName);
            }

            @Override
            public void visitInlineFragment(QueryVisitorInlineFragmentEnvironment queryVisitorInlineFragmentEnvironment) {
            }

            @Override
            public TraversalControl visitArgumentValue(QueryVisitorFieldArgumentValueEnvironment environment) {
                QueryVisitorFieldArgumentInputValue argumentInputValue = environment.getArgumentInputValue();
                if (argumentInputValue.getValue() instanceof VariableReference) {
                    String name = ((VariableReference) argumentInputValue.getValue()).getName();
                    if (!variableNames.containsKey(name)) {
                        String newName = "var" + variableCounter.getAndIncrement();
                        variableNames.put(name, newName);
                    }
                }
                return CONTINUE;
            }

            @Override
            public void visitFragmentSpread(QueryVisitorFragmentSpreadEnvironment queryVisitorFragmentSpreadEnvironment) {
                FragmentDefinition fragmentDefinition = queryVisitorFragmentSpreadEnvironment.getFragmentDefinition();
                String newName;
                if (!nodeToNewName.containsKey(fragmentDefinition)) {
                    newName = "Fragment" + fragmentCounter.getAndIncrement();
                    nodeToNewName.put(fragmentDefinition, newName);
                } else {
                    newName = nodeToNewName.get(fragmentDefinition);
                }
                nodeToNewName.put(queryVisitorFragmentSpreadEnvironment.getFragmentSpread(), newName);
            }

            @Override
            public TraversalControl visitArgument(QueryVisitorFieldArgumentEnvironment environment) {
                String newName = assertNotNull(newNames.get(environment.getGraphQLArgument()));
                nodeToNewName.put(environment.getArgument(), newName);
                return CONTINUE;
            }
        });

        AtomicInteger stringValueCounter = new AtomicInteger(1);
        AtomicInteger intValueCounter = new AtomicInteger(1);
        AstTransformer astTransformer = new AstTransformer();
        AtomicInteger aliasCounter = new AtomicInteger(1);
        Document newDocument = (Document) astTransformer.transform(document, new NodeVisitorStub() {


            @Override
            public TraversalControl visitStringValue(StringValue node, TraverserContext<Node> context) {
                return changeNode(context, node.transform(builder -> builder.value("stringValue" + stringValueCounter.getAndIncrement())));
            }

            @Override
            public TraversalControl visitIntValue(IntValue node, TraverserContext<Node> context) {
                return changeNode(context, node.transform(builder -> builder.value(BigInteger.valueOf(intValueCounter.getAndIncrement()))));
            }


            @Override
            public TraversalControl visitOperationDefinition(OperationDefinition node, TraverserContext<Node> context) {
                if (node.getName() != null) {
                    return changeNode(context, node.transform(builder -> builder.name("operation")));
                } else {
                    return CONTINUE;
                }
            }

            @Override
            public TraversalControl visitField(Field node, TraverserContext<Node> context) {
                String newAlias = null;
                if (node.getAlias() != null) {
                    newAlias = "alias" + aliasCounter.getAndIncrement();
                }
                String newName;
                if (node.getName().equals(Introspection.TypeNameMetaFieldDef.getName())) {
                    newName = Introspection.TypeNameMetaFieldDef.getName();
                } else {
                    newName = assertNotNull(nodeToNewName.get(node));
                }
                String finalNewAlias = newAlias;
                return changeNode(context, node.transform(builder -> builder.name(newName).alias(finalNewAlias)));
            }

            @Override
            public TraversalControl visitVariableDefinition(VariableDefinition node, TraverserContext<Node> context) {
                String newName = assertNotNull(variableNames.get(node.getName()));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitVariableReference(VariableReference node, TraverserContext<Node> context) {
                String newName = assertNotNull(variableNames.get(node.getName()));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitFragmentDefinition(FragmentDefinition node, TraverserContext<Node> context) {
                String newName = assertNotNull(nodeToNewName.get(node));
                GraphQLType currentCondition = assertNotNull(schema.getType(node.getTypeCondition().getName()));
                String newCondition = newNames.get(currentCondition);
                return changeNode(context, node.transform(builder -> builder.name(newName).typeCondition(new TypeName(newCondition))));
            }

            @Override
            public TraversalControl visitInlineFragment(InlineFragment node, TraverserContext<Node> context) {
                GraphQLType currentCondition = assertNotNull(schema.getType(node.getTypeCondition().getName()));
                String newCondition = newNames.get(currentCondition);
                return changeNode(context, node.transform(builder -> builder.typeCondition(new TypeName(newCondition))));
            }

            @Override
            public TraversalControl visitFragmentSpread(FragmentSpread node, TraverserContext<Node> context) {
                String newName = assertNotNull(nodeToNewName.get(node));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }

            @Override
            public TraversalControl visitArgument(Argument node, TraverserContext<Node> context) {
                String newName = assertNotNull(nodeToNewName.get(node));
                return changeNode(context, node.transform(builder -> builder.name(newName)));
            }
        });
        return AstPrinter.printAstCompact(newDocument);
    }

//    private void findAllTheSameFields(GraphQLSchema schema) {
//        Map<GraphQLFieldDefinition, Collection<GraphQLFieldDefinition>> sameFields = new LinkedHashMap<>();
//
//        GraphQLTypeVisitor visitor = new GraphQLTypeVisitorStub() {
//            @Override
//            public TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition graphQLFieldDefinition, TraverserContext<GraphQLSchemaElement> context) {
//                String curName = graphQLFieldDefinition.getName();
//                GraphQLImplementingType parentNode = (GraphQLImplementingType) context.getParentNode();
//                List<GraphQLNamedOutputType> interfaces = parentNode.getInterfaces();
//                List<GraphQLFieldDefinition> matchingInterfaceFieldDefinitions = getMatchingInterfaceFieldDefinitions(curName, interfaces);
//                if (matchingInterfaceFieldDefinitions.size() > 0) {
//                    sameFields.put(graphQLFieldDefinition, matchingInterfaceFieldDefinitions);
//                }
//            }
//
//        };
//
//        SchemaTransformer.transformSchema(schema, visitor);
//
//    }

    private static void assertUniqueOperation(Document document) {
        String operationName = null;
        for (Definition definition : document.getDefinitions()) {
            if (definition instanceof OperationDefinition) {
                if (operationName != null) {
                    throw new AssertException("Query must have exactly one operation");
                }
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                operationName = operationDefinition.getName();
            }
        }

    }

}