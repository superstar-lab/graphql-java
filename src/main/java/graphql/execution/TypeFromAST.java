package graphql.execution;


import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.*;

public class TypeFromAST {

    public static GraphQLType getTypeFromAST(GraphQLSchema schema, Type type) {
        if (type instanceof ListType) {
            return new GraphQLList(getTypeFromAST(schema, ((ListType) type).getType()));
        } else if (type instanceof NonNullType) {
            return new GraphQLNonNull(getTypeFromAST(schema, ((NonNullType) type).getType()));
        }
        return SchemaUtil.findType(schema, ((TypeName) type).getName());
    }
}
