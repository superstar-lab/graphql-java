package graphql.schema;


import java.util.List;
import java.util.Map;

public class GraphQLFieldDefinition {

    private final String name;
    private final GraphQLOutputType type;
    private final ResolveValue resolveValue;


    public GraphQLFieldDefinition(String name, GraphQLOutputType type) {
        this.name = name;
        this.type = type;
        this.resolveValue = new ResolveValue() {
            @Override
            public Object resolve(Object source, List<Object> arguments) {
                return ((Map<String, Object>) source).get(GraphQLFieldDefinition.this.name);
            }
        };
    }

    public GraphQLFieldDefinition(String name, GraphQLOutputType type, ResolveValue resolveValue) {
        this.name = name;
        this.type = type;
        this.resolveValue = resolveValue;
    }

    public GraphQLFieldDefinition(String name, GraphQLOutputType type, final Object value) {
        this.name = name;
        this.type = type;
        this.resolveValue = new ResolveValue() {
            @Override
            public Object resolve(Object source, List<Object> arguments) {
                return value;
            }
        };
    }

    public String getName() {
        return name;
    }


    public GraphQLOutputType getType() {
        return type;
    }

    public ResolveValue getResolveValue() {
        return resolveValue;
    }
}
