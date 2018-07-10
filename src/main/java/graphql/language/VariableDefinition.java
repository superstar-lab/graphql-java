package graphql.language;


import graphql.PublicApi;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PublicApi
public class VariableDefinition extends AbstractNode<VariableDefinition> implements NamedNode<VariableDefinition> {

    private final String name;
    private final Type type;
    private final Value defaultValue;

    private VariableDefinition(String name,
                               Type type,
                               Value defaultValue,
                               SourceLocation sourceLocation,
                               List<Comment> comments) {
        super(sourceLocation, comments);
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public Value getDefaultValue() {
        return defaultValue;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        result.add(type);
        if (defaultValue != null) result.add(defaultValue);
        return result;
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableDefinition that = (VariableDefinition) o;

        return NodeUtil.isEqualTo(this.name, that.name);

    }

    @Override
    public VariableDefinition deepCopy() {
        return new VariableDefinition(name,
                deepCopy(type),
                deepCopy(defaultValue),
                getSourceLocation(),
                getComments()
        );
    }

    @Override
    public String toString() {
        return "VariableDefinition{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", defaultValue=" + defaultValue +
                '}';
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return visitor.visitVariableDefinition(this, context);
    }


    public static Builder newVariableDefinition() {
        return new Builder();
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private String name;
        private List<Comment> comments = Collections.emptyList();
        private Type type;
        private Value defaultValue;

        private Builder() {
        }


        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder comments(List<Comment> comments) {
            this.comments = comments;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder defaultValue(Value defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public VariableDefinition build() {
            VariableDefinition variableDefinition = new VariableDefinition(
                    name,
                    type,
                    defaultValue,
                    sourceLocation,
                    comments
            );
            return variableDefinition;
        }
    }
}
