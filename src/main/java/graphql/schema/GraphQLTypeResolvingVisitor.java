package graphql.schema;

import graphql.Assert;
import graphql.Internal;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.Map;
import java.util.stream.Collectors;

@Internal
public class GraphQLTypeResolvingVisitor extends GraphQLTypeVisitorStub {
    protected final Map<String, GraphQLType> typeMap;

    public GraphQLTypeResolvingVisitor(Map<String, GraphQLType> typeMap) {
        this.typeMap = typeMap;
    }

    @Override
    public TraversalControl visitGraphQLObjectType(GraphQLObjectType node, TraverserContext<GraphQLType> context) {

        node.replaceInterfaces(node.getInterfaces().stream()
                .map(type -> (GraphQLOutputType)typeMap.get(type.getName()))
                .collect(Collectors.toList()));
        return super.visitGraphQLObjectType(node, context);
    }

    @Override
    public TraversalControl visitGraphQLUnionType(GraphQLUnionType node, TraverserContext<GraphQLType> context) {

        node.replaceTypes(node.getTypes().stream()
                .map(type -> (GraphQLOutputType)typeMap.get(type.getName()))
                .collect(Collectors.toList()));
        return super.visitGraphQLUnionType(node, context);
    }

    @Override
    public TraversalControl visitGraphQLTypeReference(GraphQLTypeReference node, TraverserContext<GraphQLType> context) {

        final GraphQLType resolvedType = typeMap.get(node.getName());
        Assert.assertTrue(resolvedType != null, "type %s not found in schema", node.getName());
        context.getParentContext().thisNode().accept(context, new GraphQLTypeRefResolvingVisitor(resolvedType) );
        return super.visitGraphQLTypeReference(node, context);
    }
}
