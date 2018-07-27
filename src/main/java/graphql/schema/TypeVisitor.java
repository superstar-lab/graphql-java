package graphql.schema;

import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

public interface TypeVisitor {
    TraversalControl visitGraphQLArgument(GraphQLArgument node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLInterfaceType(GraphQLInterfaceType node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLEnumType(GraphQLEnumType node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLEnumValueDefinition(GraphQLEnumValueDefinition node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLFieldDefinition(GraphQLFieldDefinition node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLInputObjectField(GraphQLInputObjectField node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLInputObjectType(GraphQLInputObjectType node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLList(GraphQLList node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLNonNull(GraphQLNonNull node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLObjectType(GraphQLObjectType node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLScalarType(GraphQLScalarType node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLTypeReference(GraphQLTypeReference node, TraverserContext<GraphQLType> context);
    TraversalControl visitGraphQLUnionType(GraphQLUnionType node, TraverserContext<GraphQLType> context);

    // Marker interfaces
    default TraversalControl visitGraphQLModifiedType(GraphQLModifiedType node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLCompositeType(GraphQLCompositeType node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLDirectiveContainer(GraphQLDirectiveContainer node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLFieldsContainer(GraphQLFieldsContainer node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLInputFieldsContainer(GraphQLInputFieldsContainer node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLInputType(GraphQLInputType node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLNullableType(GraphQLNullableType node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLOutputType(GraphQLOutputType node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }

    default TraversalControl visitGraphQLUnmodifiedType(GraphQLUnmodifiedType node, TraverserContext<GraphQLType> context) {
        throw new UnsupportedOperationException();
    }


}
