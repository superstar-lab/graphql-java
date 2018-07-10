package graphql.language;


import graphql.PublicApi;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PublicApi
public class DirectiveDefinition extends AbstractNode<DirectiveDefinition> implements Definition<DirectiveDefinition>, NamedNode<DirectiveDefinition> {
    private final String name;
    private Description description;
    private final List<InputValueDefinition> inputValueDefinitions;
    private final List<DirectiveLocation> directiveLocations;


    private DirectiveDefinition(String name,
                                List<InputValueDefinition> inputValueDefinitions,
                                List<DirectiveLocation> directiveLocations,
                                SourceLocation sourceLocation,
                                List<Comment> comments
    ) {
        super(sourceLocation, comments);
        this.name = name;
        this.inputValueDefinitions = inputValueDefinitions;
        this.directiveLocations = directiveLocations;
    }

    @Override
    public String getName() {
        return name;
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public List<InputValueDefinition> getInputValueDefinitions() {
        return new ArrayList<>(inputValueDefinitions);
    }

    public List<DirectiveLocation> getDirectiveLocations() {
        return new ArrayList<>(directiveLocations);
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        result.addAll(inputValueDefinitions);
        result.addAll(directiveLocations);
        return result;
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DirectiveDefinition that = (DirectiveDefinition) o;

        return NodeUtil.isEqualTo(this.name, that.name);
    }

    @Override
    public DirectiveDefinition deepCopy() {
        return new DirectiveDefinition(name,
                deepCopy(inputValueDefinitions),
                deepCopy(directiveLocations),
                getSourceLocation(),
                getComments());
    }

    @Override
    public String toString() {
        return "DirectiveDefinition{" +
                "name='" + name + "'" +
                ", inputValueDefinitions=" + inputValueDefinitions +
                ", directiveLocations=" + directiveLocations +
                "}";
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return visitor.visitDirectiveDefinition(this, context);
    }

    public static Builder newDirectiveDefinition() {
        return new Builder();
    }


    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = Collections.emptyList();
        private String name;
        private Description description;
        private List<InputValueDefinition> inputValueDefinitions = new ArrayList<>();
        private List<DirectiveLocation> directiveLocations = new ArrayList<>();

        private Builder() {
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

        public Builder description(Description description) {
            this.description = description;
            return this;
        }

        public Builder inputValueDefinitions(List<InputValueDefinition> inputValueDefinitions) {
            this.inputValueDefinitions = inputValueDefinitions;
            return this;
        }

        public Builder inputValueDefinition(InputValueDefinition inputValueDefinition) {
            this.inputValueDefinitions.add(inputValueDefinition);
            return this;
        }

        public Builder directiveLocations(List<DirectiveLocation> directiveLocations) {
            this.directiveLocations = directiveLocations;
            return this;
        }

        public Builder directiveLocation(DirectiveLocation directiveLocation) {
            this.directiveLocations.add(directiveLocation);
            return this;
        }

        public DirectiveDefinition build() {
            DirectiveDefinition directiveDefinition = new DirectiveDefinition(name, inputValueDefinitions, directiveLocations, sourceLocation, comments);
            directiveDefinition.setDescription(description);
            return directiveDefinition;
        }
    }
}
