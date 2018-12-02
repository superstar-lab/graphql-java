package graphql.language;


import graphql.Internal;
import graphql.PublicApi;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@PublicApi
public class BooleanValue extends AbstractNode<BooleanValue> implements ScalarValue<BooleanValue> {

    private final boolean value;

    @Internal
    protected BooleanValue(boolean value, SourceLocation sourceLocation, List<Comment> comments, IgnoredChars ignoredChars) {
        super(sourceLocation, comments, ignoredChars);
        this.value = value;
    }

    /**
     * alternative to using a Builder for convenience
     *
     * @param value of the Boolean
     */
    public BooleanValue(boolean value) {
        this(value, null, new ArrayList<>(), IgnoredChars.EMPTY);
    }

    public boolean isValue() {
        return value;
    }

    @Override
    public List<Node> getChildren() {
        return new ArrayList<>();
    }

    @Override
    public NodeChildrenContainer getNamedChildren() {
        return NodeChildrenContainer.newNodeChildrenContainer().build();
    }

    @Override
    public BooleanValue withNewChildren(NodeChildrenContainer newChildren) {
        if (!newChildren.isEmpty()) {
            throw new IllegalArgumentException("Cannot pass non-empty newChildren to Node that doesn't hold children");
        }

        return this;
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BooleanValue that = (BooleanValue) o;

        return value == that.value;

    }

    @Override
    public BooleanValue deepCopy() {
        return new BooleanValue(value, getSourceLocation(), getComments(), getIgnoredChars());
    }

    @Override
    public String toString() {
        return "BooleanValue{" +
                "value=" + value +
                '}';
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return visitor.visitBooleanValue(this, context);
    }

    public static Builder newBooleanValue() {
        return new Builder();
    }


    public static Builder newBooleanValue(boolean value) {
        return new Builder().value(value);
    }

    public BooleanValue transform(Consumer<Builder> builderConsumer) {
        Builder builder = new Builder(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private boolean value;
        private List<Comment> comments = new ArrayList<>();
        private IgnoredChars ignoredChars = IgnoredChars.EMPTY;

        private Builder() {
        }

        private Builder(BooleanValue existing) {
            this.sourceLocation = existing.getSourceLocation();
            this.comments = existing.getComments();
            this.value = existing.isValue();
            this.ignoredChars = existing.getIgnoredChars();
        }


        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder value(boolean value) {
            this.value = value;
            return this;
        }

        public Builder comments(List<Comment> comments) {
            this.comments = comments;
            return this;
        }

        public Builder ignoredChars(IgnoredChars ignoredChars) {
            this.ignoredChars = ignoredChars;
            return this;
        }

        public BooleanValue build() {
            BooleanValue booleanValue = new BooleanValue(value, sourceLocation, comments, ignoredChars);
            return booleanValue;
        }
    }
}
