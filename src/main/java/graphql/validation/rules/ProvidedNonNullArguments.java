package graphql.validation.rules;


import graphql.Internal;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.NullValue;
import graphql.language.Value;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.validation.AbstractRule;
import graphql.validation.ValidationContext;
import graphql.validation.ValidationErrorCollector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLTypeUtil.isNonNull;
import static graphql.validation.ValidationErrorType.MissingDirectiveArgument;
import static graphql.validation.ValidationErrorType.MissingFieldArgument;
import static graphql.validation.ValidationErrorType.NullValueForNonNullArgument;

@Internal
public class ProvidedNonNullArguments extends AbstractRule {

    public ProvidedNonNullArguments(ValidationContext validationContext, ValidationErrorCollector validationErrorCollector) {
        super(validationContext, validationErrorCollector);
    }

    @Override
    public void checkField(Field field) {
        GraphQLFieldDefinition fieldDef = getValidationContext().getFieldDef();
        if (fieldDef == null) {
            return;
        }
        Map<String, Argument> argumentMap = argumentMap(field.getArguments());

        for (GraphQLArgument graphQLArgument : fieldDef.getArguments()) {
            Argument argument = argumentMap.get(graphQLArgument.getName());
            boolean nonNullType = isNonNull(graphQLArgument.getType());
            boolean noDefaultValue = graphQLArgument.getArgumentDefaultValue().isNotSet();
            if (argument == null && nonNullType && noDefaultValue) {
                String message = i18n(MissingFieldArgument, "ProvidedNonNullArguments.missingFieldArg", graphQLArgument.getName());
                addError(MissingFieldArgument, field.getSourceLocation(), message);
            }

            if (argument != null) {
                Value value = argument.getValue();
                if ((value == null || value instanceof NullValue) && nonNullType && noDefaultValue) {
                    String message = i18n(NullValueForNonNullArgument, "ProvidedNonNullArguments.nullValue", graphQLArgument.getName());
                    addError(NullValueForNonNullArgument, field.getSourceLocation(), message);
                }
            }
        }
    }


    @Override
    public void checkDirective(Directive directive, List<Node> ancestors) {
        GraphQLDirective graphQLDirective = getValidationContext().getDirective();
        if (graphQLDirective == null) {
            return;
        }
        Map<String, Argument> argumentMap = argumentMap(directive.getArguments());

        for (GraphQLArgument graphQLArgument : graphQLDirective.getArguments()) {
            Argument argument = argumentMap.get(graphQLArgument.getName());
            boolean nonNullType = isNonNull(graphQLArgument.getType());
            boolean noDefaultValue = graphQLArgument.getArgumentDefaultValue().isNotSet();
            if (argument == null && nonNullType && noDefaultValue) {
                String message = i18n(MissingDirectiveArgument, "ProvidedNonNullArguments.missingDirectiveArg", graphQLArgument.getName());
                addError(MissingDirectiveArgument, directive.getSourceLocation(), message);
            }
        }
    }

    private Map<String, Argument> argumentMap(List<Argument> arguments) {
        Map<String, Argument> result = new LinkedHashMap<>();
        for (Argument argument : arguments) {
            result.put(argument.getName(), argument);
        }
        return result;
    }
}
