package graphql.validation.rules;


import graphql.language.TypeName;
import graphql.validation.*;

public class KnownTypeNames extends AbstractRule {


    public KnownTypeNames(ValidationContext validationContext, ValidationErrorCollector validationErrorCollector) {
        super(validationContext, validationErrorCollector);
    }

    @Override
    public void checkTypeName(TypeName typeName) {
        if ((getValidationContext().getSchema().getType(typeName.getName())) == null) {
            String message = String.format("Unknown type %s", typeName.getName());
            addError(new ValidationError(ValidationErrorType.UnknownType, typeName.getSourceLocation(), message));
        }
    }
}
