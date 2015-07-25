package graphql.validation.rules;


import graphql.language.FragmentDefinition;
import graphql.language.InlineFragment;
import graphql.schema.GraphQLCompositeType;
import graphql.schema.GraphQLType;
import graphql.schema.SchemaUtil;
import graphql.validation.*;

public class FragmentsOnCompositeType extends AbstractRule {

    private SchemaUtil schemaUtil = new SchemaUtil();

    public FragmentsOnCompositeType(ValidationContext validationContext, ValidationErrorCollector validationErrorCollector) {
        super(validationContext, validationErrorCollector);
    }

    @Override
    public void checkInlineFragment(InlineFragment inlineFragment) {
        GraphQLType type = schemaUtil.findType(getValidationContext().getSchema(), inlineFragment.getTypeCondition().getName());
        if (type == null) return;
        if (!(type instanceof GraphQLCompositeType)) {
            addError(new ValidationError(ValidationErrorType.InlineFragmentTypeConditionInvalid));
        }
    }

    @Override
    public void checkFragmentDefinition(FragmentDefinition fragmentDefinition) {
        GraphQLType type = schemaUtil.findType(getValidationContext().getSchema(), fragmentDefinition.getTypeCondition().getName());
        if (type == null) return;
        if (!(type instanceof GraphQLCompositeType)) {
            addError(new ValidationError(ValidationErrorType.InlineFragmentTypeConditionInvalid));
        }
    }
}
