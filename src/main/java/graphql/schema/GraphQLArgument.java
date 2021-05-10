package graphql.schema;


import graphql.DirectivesUtil;
import graphql.Internal;
import graphql.PublicApi;
import graphql.execution.ValuesResolver;
import graphql.language.InputValueDefinition;
import graphql.language.Value;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertValidName;

/**
 * This defines an argument that can be supplied to a graphql field (via {@link graphql.schema.GraphQLFieldDefinition}.
 * <p>
 * Fields can be thought of as "functions" that take arguments and return a value.
 * <p>
 * See http://graphql.org/learn/queries/#arguments for more details on the concept.
 * <p>
 * {@link graphql.schema.GraphQLArgument} is used in two contexts, one context is graphql queries where it represents the arguments that can be
 * set on a field and the other is in Schema Definition Language (SDL) where it can be used to represent the argument value instances
 * that have been supplied on a {@link graphql.schema.GraphQLDirective}.
 * <p>
 * The difference is the 'value' and 'defaultValue' properties.  In a query argument, the 'value' is never in the GraphQLArgument
 * object but rather in the AST direct or in the query variables map and the 'defaultValue' represents a value to use if both of these are
 * not present. You can think of them like a descriptor of what shape an argument might have.
 * <p>
 * However with directives on SDL elements, the value is specified in AST only and transferred into the GraphQLArgument object and the
 * 'defaultValue' comes instead from the directive definition elsewhere in the SDL.  You can think of them as 'instances' of arguments, their shape and their
 * specific value on that directive.
 */
@PublicApi
public class GraphQLArgument implements GraphQLNamedSchemaElement, GraphQLInputValueDefinition {

    private final String name;
    private final String description;
    private final String deprecationReason;
    private final GraphQLInputType originalType;

    /**
     * This should normally always be a Value (Ast Literal),
     * but in order to preserve backwards compatibility
     * we accept Objects and treat is as already coerced internal
     * input values.
     */
    private final Object defaultValue;
    /**
     * This should normally always be a Value (Ast Literal),
     * but in order to preserve backwards compatibility
     * we accept Objects and treat is as already coerced internal
     * input values.
     */
    private final Object value;

    private final InputValueDefinition definition;
    private final DirectivesUtil.DirectivesHolder directives;

    private GraphQLInputType replacedType;

    public static final String CHILD_DIRECTIVES = "directives";
    public static final String CHILD_TYPE = "type";

    private static final Object VALUE_SENTINEL = new Object() {
    };

    /**
     * @param name         the arg name
     * @param description  the arg description
     * @param type         the arg type
     * @param defaultValue the default value
     *
     * @deprecated use the {@link #newArgument()} builder pattern instead, as this constructor will be made private in a future version.
     */
    @Internal
    @Deprecated
    public GraphQLArgument(String name, String description, GraphQLInputType type, Object defaultValue) {
        this(name, description, type, defaultValue, null);
    }

    /**
     * @param name the arg name
     * @param type the arg type
     *
     * @deprecated use the {@link #newArgument()} builder pattern instead, as this constructor will be made private in a future version.
     */
    @Internal
    @Deprecated
    public GraphQLArgument(String name, GraphQLInputType type) {
        this(name, null, type, VALUE_SENTINEL, null);
    }

    /**
     * @param name         the arg name
     * @param description  the arg description
     * @param type         the arg type
     * @param defaultValue the default value
     * @param definition   the AST definition
     *
     * @deprecated use the {@link #newArgument()} builder pattern instead, as this constructor will be made private in a future version.
     */
    public GraphQLArgument(String name, String description, GraphQLInputType type, Object defaultValue, InputValueDefinition definition) {
        this(name, description, type, defaultValue, null, definition, Collections.emptyList(), null);
    }

    private GraphQLArgument(String name, String description, GraphQLInputType type, Object defaultValue, Object value, InputValueDefinition definition, List<GraphQLDirective> directives, String deprecationReason) {
        assertValidName(name);
        assertNotNull(type, () -> "type can't be null");
        this.name = name;
        this.description = description;
        this.originalType = type;
        this.defaultValue = defaultValue;
        this.value = value;
        this.definition = definition;
        this.deprecationReason = deprecationReason;
        this.directives = new DirectivesUtil.DirectivesHolder(directives);
    }


    void replaceType(GraphQLInputType type) {
        this.replacedType = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public GraphQLInputType getType() {
        return replacedType != null ? replacedType : originalType;
    }

    /**
     * An argument has a default value when it represents the logical argument structure that a {@link graphql.schema.GraphQLFieldDefinition}
     * can have and it can also have a default value when used in a schema definition language (SDL) where the
     * default value comes via the directive definition.
     *
     * @return the default value of an argument. Should normally be a Value (or null), but can be an arbitrary object for legacy code.
     */
    public @Nullable Object getDefaultValue() {
        return defaultValue == VALUE_SENTINEL ? null : defaultValue;
    }

    public boolean hasSetDefaultValue() {
        return defaultValue != VALUE_SENTINEL;
    }

    /**
     * An argument ONLY has a value when its used in a schema definition language (SDL) context as the arguments to SDL directives.  The method
     * should not be called in a query context, but rather the AST / variables map should be used to obtain an arguments value.
     *
     * @return the argument value. Should normally be a Value (or null), but can be an arbitrary object for legacy code.
     */
    public @Nullable Object getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public boolean isDeprecated() {
        return deprecationReason != null;
    }

    public InputValueDefinition getDefinition() {
        return definition;
    }

    @Override
    public List<GraphQLDirective> getDirectives() {
        return directives.getDirectives();
    }

    @Override
    public Map<String, GraphQLDirective> getDirectivesByName() {
        return directives.getDirectivesByName();
    }

    @Override
    public Map<String, List<GraphQLDirective>> getAllDirectivesByName() {
        return directives.getAllDirectivesByName();
    }

    @Override
    public GraphQLDirective getDirective(String directiveName) {
        return directives.getDirective(directiveName);
    }

    @Override
    public List<GraphQLSchemaElement> getChildren() {
        List<GraphQLSchemaElement> children = new ArrayList<>();
        children.add(getType());
        children.addAll(directives.getDirectives());
        return children;
    }


    @Override
    public SchemaElementChildrenContainer getChildrenWithTypeReferences() {
        return SchemaElementChildrenContainer.newSchemaElementChildrenContainer()
                .children(CHILD_DIRECTIVES, directives.getDirectives())
                .child(CHILD_TYPE, originalType)
                .build();
    }

    @Override
    public GraphQLArgument withNewChildren(SchemaElementChildrenContainer newChildren) {
        return transform(builder ->
                builder.type(newChildren.getChildOrNull(CHILD_TYPE))
                        .replaceDirectives(newChildren.getChildren(CHILD_DIRECTIVES)));
    }

    @Override
    public GraphQLSchemaElement copy() {
        return newArgument(this).build();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }


    /**
     * This helps you transform the current GraphQLArgument into another one by starting a builder with all
     * the current values and allows you to transform it how you want.
     *
     * @param builderConsumer the consumer code that will be given a builder to transform
     *
     * @return a new field based on calling build on that builder
     */
    public GraphQLArgument transform(Consumer<Builder> builderConsumer) {
        Builder builder = newArgument(this);
        builderConsumer.accept(builder);
        return builder.build();
    }

    public static Builder newArgument() {
        return new Builder();
    }

    public static Builder newArgument(GraphQLArgument existing) {
        return new Builder(existing);
    }

    @Override
    public TraversalControl accept(TraverserContext<GraphQLSchemaElement> context, GraphQLTypeVisitor visitor) {
        return visitor.visitGraphQLArgument(this, context);
    }

    @Override
    public String toString() {
        return "GraphQLArgument{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", defaultValue=" + defaultValue +
                ", type=" + getType() +
                '}';
    }

    public static class Builder extends GraphqlTypeBuilder {

        private GraphQLInputType type;
        private Object defaultValue = VALUE_SENTINEL;
        private Object value = VALUE_SENTINEL;
        private ValueState defaultValueState = ValueState.NOT_SET;
        private ValueState valueState = ValueState.NOT_SET;
        private String deprecationReason;
        private InputValueDefinition definition;
        private final List<GraphQLDirective> directives = new ArrayList<>();

        private enum ValueState {
            NOT_SET,
            LITERAL,
            EXTERNAL_VALUE,
            INTERNAL_VALUE // this is deprecated and should not be used going forward
        }


        public Builder() {
        }

        public Builder(GraphQLArgument existing) {
            this.name = existing.getName();
            this.type = existing.originalType;
            this.value = existing.getValue();
            if (this.value instanceof Value) {
                this.valueState = ValueState.LITERAL;
            } else {
                this.valueState = ValueState.INTERNAL_VALUE;
            }
            this.defaultValue = existing.defaultValue;
            if (this.defaultValue instanceof Value) {
                this.defaultValueState = ValueState.LITERAL;
            } else {
                this.defaultValueState = ValueState.INTERNAL_VALUE;
            }
            this.description = existing.getDescription();
            this.definition = existing.getDefinition();
            this.deprecationReason = existing.deprecationReason;
            DirectivesUtil.enforceAddAll(this.directives, existing.getDirectives());
        }

        @Override
        public Builder name(String name) {
            super.name(name);
            return this;
        }

        @Override
        public Builder description(String description) {
            super.description(description);
            return this;
        }

        @Override
        public Builder comparatorRegistry(GraphqlTypeComparatorRegistry comparatorRegistry) {
            super.comparatorRegistry(comparatorRegistry);
            return this;
        }

        public Builder definition(InputValueDefinition definition) {
            this.definition = definition;
            return this;
        }

        public Builder deprecate(String deprecationReason) {
            this.deprecationReason = deprecationReason;
            return this;
        }

        public Builder type(GraphQLInputType type) {
            this.type = type;
            return this;
        }

        /**
         * @param defaultValue
         *
         * @return
         *
         * @deprecated use {@link #defaultValueLiteral(Value)} or {@link #defaultValueProgrammatic(Object)}
         */
        @Deprecated
        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            this.defaultValueState = ValueState.INTERNAL_VALUE;
            return this;
        }

        /**
         * @param defaultValue can't be null as a `null` is represented a @{@link graphql.language.NullValue} Literal
         *
         * @return
         */
        public Builder defaultValueLiteral(@NotNull Value defaultValue) {
            this.defaultValue = assertNotNull(defaultValue, () -> "defaultValue can't be null");
            this.defaultValueState = ValueState.LITERAL;
            return this;
        }

        /**
         * @param defaultValue Can be null to represent null value
         *
         * @return
         */
        public Builder defaultValueProgrammatic(@Nullable Object defaultValue) {
            this.defaultValue = defaultValue;
            this.defaultValueState = ValueState.EXTERNAL_VALUE;
            return this;
        }

        /**
         * Removes the defaultValue to represent a missing default value (which is different from null)
         *
         * @return
         */
        public Builder clearDefaultValue() {
            this.defaultValue = VALUE_SENTINEL;
            this.defaultValueState = ValueState.NOT_SET;
            return this;
        }

        /**
         * @param value
         *
         * @return
         *
         * @deprecated use {@link #valueLiteral(Value)} or {@link #valueProgrammatic(Object)}
         */
        @Deprecated
        public Builder value(Object value) {
            this.value = value;
            this.valueState = ValueState.INTERNAL_VALUE;
            return this;
        }

        /**
         * @param value can't be null as a `null` is represented a @{@link graphql.language.NullValue} Literal
         *
         * @return defaultValue Can be null to represent null value
         */
        public Builder valueLiteral(@NotNull Value value) {
            this.value = value;
            this.value = ValueState.LITERAL;
            return this;
        }

        /**
         * @param
         *
         * @return values can be null to represent null value
         */
        public Builder valueProgrammatic(@Nullable Object value) {
            this.value = value;
            this.value = ValueState.EXTERNAL_VALUE;
            return this;
        }

        /**
         * Removes the value to represent a missing value (which is different from null)
         *
         * @return
         */
        public Builder clearValue() {
            this.value = null;
            this.valueState = ValueState.NOT_SET;
            return this;
        }

        public Builder withDirectives(GraphQLDirective... directives) {
            assertNotNull(directives, () -> "directives can't be null");
            this.directives.clear();
            for (GraphQLDirective directive : directives) {
                withDirective(directive);
            }
            return this;
        }

        public Builder withDirective(GraphQLDirective directive) {
            assertNotNull(directive, () -> "directive can't be null");
            DirectivesUtil.enforceAdd(this.directives, directive);
            return this;
        }

        public Builder replaceDirectives(List<GraphQLDirective> directives) {
            assertNotNull(directives, () -> "directive can't be null");
            this.directives.clear();
            DirectivesUtil.enforceAddAll(this.directives, directives);
            return this;
        }

        public Builder withDirective(GraphQLDirective.Builder builder) {
            return withDirective(builder.build());
        }

        /**
         * This is used to clear all the directives in the builder so far.
         *
         * @return the builder
         */
        public Builder clearDirectives() {
            directives.clear();
            return this;
        }


        public GraphQLArgument build() {
            assertNotNull(type, () -> "type can't be null");
            if (defaultValueState == ValueState.EXTERNAL_VALUE) {
                defaultValue = ValuesResolver.externalInputValueToLiteral(defaultValue, type);
            }
            if (valueState == ValueState.EXTERNAL_VALUE) {
                value = ValuesResolver.externalInputValueToLiteral(value, type);
            }

            return new GraphQLArgument(
                    name,
                    description,
                    type,
                    defaultValue,
                    value,
                    definition,
                    sort(directives, GraphQLArgument.class, GraphQLDirective.class),
                    deprecationReason
            );
        }
    }
}
