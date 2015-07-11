package graphql;


import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

public class Scalars {


    public static GraphQLScalarType GraphQLInt = new GraphQLScalarType("Int", "Built-in Int", new Coercing() {
        @Override
        public Object coerce(Object input) {
            if (input instanceof String) {
                return Integer.parseInt((String) input);
            } else if (input instanceof Integer) {
                return input;
            } else {
                throw new GraphQLException("");
            }
        }

        @Override
        public Object coerceLiteral(Object input) {
            // TODO: Should not exceed Integer ranges
            return ((IntValue) input).getValue();
        }
    });

    public static GraphQLScalarType GraphQLFloat = new GraphQLScalarType("Float", "Built-in Float", new Coercing() {
        @Override
        public Object coerce(Object input) {
            if (input instanceof String) {
                return Float.parseFloat((String) input);
            } else if (input instanceof Float) {
                return input;
            } else {
                throw new GraphQLException();
            }
        }

        @Override
        public Object coerceLiteral(Object input) {
            return ((FloatValue) input).getValue().floatValue();
        }
    });

    public static GraphQLScalarType GraphQLString = new GraphQLScalarType("String", "Built-in String", new Coercing() {
        @Override
        public Object coerce(Object input) {
            return input;
        }

        @Override
        public Object coerceLiteral(Object input) {
            return ((StringValue) input).getValue();
        }
    });


    public static GraphQLScalarType GraphQLBoolean = new GraphQLScalarType("Boolean", "Built-in Boolean", new Coercing() {
        @Override
        public Object coerce(Object input) {
            return input;
        }

        @Override
        public Object coerceLiteral(Object input) {
            return input;
        }
    });


    public static GraphQLScalarType GraphQLID = new GraphQLScalarType("ID", "Built-in ID", new Coercing() {
        @Override
        public Object coerce(Object input) {
            return input;
        }

        @Override
        public Object coerceLiteral(Object input) {
            return input;
        }
    });

}
