package graphql.language;


import graphql.PublicApi;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * This is provided to a DataFetcher, therefore it is a public API.
 * This might change in the future.
 */
@PublicApi
public class Field extends AbstractNode<Field> implements Selection<Field>, SelectionSetContainer<Field>, DirectivesContainer<Field> {

    private final String name;
    private final String alias;
    private final List<Argument> arguments;
    private final List<Directive> directives;
    private final SelectionSet selectionSet;

    public Field() {
        this(null, null, new ArrayList<>(), new ArrayList<>(), null);
    }

    public Field(String name) {
        this(name, null, new ArrayList<>(), new ArrayList<>(), null);
    }

    public Field(String name, SelectionSet selectionSet) {
        this(name, null, new ArrayList<>(), new ArrayList<>(), selectionSet);
    }


    public Field(String name, List<Argument> arguments) {
        this(name, null, arguments, new ArrayList<>(), null);
    }

    public Field(String name, List<Argument> arguments, List<Directive> directives) {
        this(name, null, arguments, directives, null);
    }

    public Field(String name, List<Argument> arguments, SelectionSet selectionSet) {
        this(name, null, arguments, new ArrayList<>(), selectionSet);
    }

    public Field(String name, String alias, List<Argument> arguments, List<Directive> directives, SelectionSet selectionSet) {
        this.name = name;
        this.alias = alias;
        this.arguments = arguments;
        this.directives = directives;
        this.selectionSet = selectionSet;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        result.addAll(arguments);
        result.addAll(directives);
        if (selectionSet != null) result.add(selectionSet);
        return result;
    }


    @Override
    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    @Override
    public List<Directive> getDirectives() {
        return directives;
    }

    @Override
    public SelectionSet getSelectionSet() {
        return selectionSet;
    }

    @Override
    public boolean isEqualTo(Node o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Field that = (Field) o;

        return NodeUtil.isEqualTo(this.name, that.name) && NodeUtil.isEqualTo(this.alias, that.alias);
    }

    @Override
    public Field deepCopy() {
        return new Field(name,
                alias,
                deepCopy(arguments),
                deepCopy(directives),
                deepCopy(selectionSet)
        );
    }

    @Override
    public String toString() {
        return "Field{" +
                "name='" + name + '\'' +
                ", alias='" + alias + '\'' +
                ", arguments=" + arguments +
                ", directives=" + directives +
                ", selectionSet=" + selectionSet +
                '}';
    }

    @Override
    public TraversalControl accept(TraverserContext<Node> context, NodeVisitor visitor) {
        return visitor.visitField(this, context);
    }

    public static Builder newField() {
        return new Builder();
    }

    public static final class Builder implements NodeBuilder {
        private SourceLocation sourceLocation;
        private List<Comment> comments = Collections.emptyList();
        private String name;
        private String alias;
        private List<Argument> arguments = new ArrayList<>();
        private List<Directive> directives = new ArrayList<>();
        private SelectionSet selectionSet;

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

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder arguments(List<Argument> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder directives(List<Directive> directives) {
            this.directives = directives;
            return this;
        }

        public Builder selectionSet(SelectionSet selectionSet) {
            this.selectionSet = selectionSet;
            return this;
        }

        public Field build() {
            Field field = new Field(name, alias, arguments, directives, selectionSet);
            field.setSourceLocation(sourceLocation);
            field.setComments(comments);
            return field;
        }
    }
}
