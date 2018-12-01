package graphql.language;


import graphql.Internal;
import graphql.PublicApi;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@PublicApi
public class OperationTypeDefinition extends AbstractNode<OperationTypeDefinition> implements NamedNode<OperationTypeDefinition> {

    private final String name;
    private final Type type;

    private static final String CHILD_TYPE = "type";

    @Internal
    protected OperationTypeDefinition(String name, Type type, SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars) {
        super(sourceLocation, comments, ignoredChars);
        this.name = name;
        this.type = type;
    }

    /**
     * alternative to using a Builder for convenience
     *
     * @param name of the operation
     * @param type the type in play
     */
    public OperationTypeDefinition(String name, Type type) {
        this(name, type, null, new ArrayList<>(), IgnoredChars.EMPTY);
    }

    public Type getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        result.add(type);
        return result;
    }

    @Override
    public ChildrenContainer getNamedChildren() {
        return ChildrenContainer.newChildrenContainer()
                .child(CHILD_TYPE, type)
                .build();
    }

    @Override
    public OperationTypeDefinition withNewChildren(ChildrenContainer newChildren) {
        return transform(builder -> builder
                .type(newChildren.getSingleValueOrNull(CHILD_TYPE))
        );
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OperationTypeDefinition that = (OperationTypeDefinition) o;

        return NodeUtil.isEqualTo(this.name, that.name);
    }

    @Override
    public OperationTypeDefinition deepCopy() {
        return new OperationTypeDefinition(name, deepCopy(type), getSourceLocation(), getComments(), getIgnoredChars());
    }

    @Override
    public String toString() {
        return "OperationTypeDefinition{" +
                "name='" + name + "'" +
                ", type=" + type +
                "}";
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return visitor.visitOperationTypeDefinition(this, context);
    }

    public static Builder newOperationTypeDefinition() {
        return new Builder();
    }

    public OperationTypeDefinition transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = new ArrayList<>();
        private String name;
        private Type type;
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;

        private Builder() {
        }


        private Builder(OperationTypeDefinition existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.name = existing.getName();
            this.type = existing.getType();
            this.ignoredChars = existing.getIgnoredChars();
        }


        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder comments(List<Comment> comments) {
            this.comments = comments;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder ignoredChars(IgnoredChars ignoredChars) {
            this.ignoredChars = ignoredChars;
            return this;
        }

        public OperationTypeDefinition build() {
            OperationTypeDefinition operationTypeDefinition = new OperationTypeDefinition(name, type, sourceLocation, comments, ignoredChars);
            return operationTypeDefinition;
        }
    }
}
