package graphql.schema.validation;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;

import java.util.List;

import static graphql.schema.GraphQLTypeUtil.getUnwrappedTypeName;
import static graphql.schema.validation.SchemaValidationErrorType.ObjectDoesNotImplementItsInterfaces;
import static java.lang.String.format;

/**
 * Schema validation rule ensuring object types have all the fields that they need to implement the interfaces
 * they say they implement
 */
public class ObjectsImplementInterfaces implements SchemaValidationRule {

    @Override
    public void check(GraphQLFieldDefinition fieldDef, SchemaValidationErrorCollector validationErrorCollector) {
    }

    @Override
    public void check(GraphQLType type, SchemaValidationErrorCollector validationErrorCollector) {
        if (type instanceof GraphQLObjectType) {
            check((GraphQLObjectType) type, validationErrorCollector);
        }
    }

    // visible for testing
    private void check(GraphQLObjectType objectTyoe, SchemaValidationErrorCollector validationErrorCollector) {
        List<GraphQLOutputType> interfaces = objectTyoe.getInterfaces();
        interfaces.forEach(interfaceType -> {
            // we have resolved the interfaces at this point and hence the cast is ok
            checkObjectImplementsInterface(objectTyoe, (GraphQLInterfaceType) interfaceType, validationErrorCollector);
        });

    }

    private void checkObjectImplementsInterface(GraphQLObjectType objectTyoe, GraphQLInterfaceType interfaceType, SchemaValidationErrorCollector validationErrorCollector) {
        List<GraphQLFieldDefinition> fieldDefinitions = interfaceType.getFieldDefinitions();
        for (GraphQLFieldDefinition interfaceFieldDef : fieldDefinitions) {
            GraphQLFieldDefinition objectFieldDef = objectTyoe.getFieldDefinition(interfaceFieldDef.getName());
            if (objectFieldDef == null) {
                validationErrorCollector.addError(
                        error(format("object type '%s' does not implement interface '%s' because field '%s' is missing",
                                objectTyoe.getName(), interfaceType.getName(), interfaceFieldDef.getName())));
            } else {
                checkFieldTypeEquivalence(objectTyoe, interfaceType, validationErrorCollector, interfaceFieldDef, objectFieldDef);
            }
        }
    }

    private void checkFieldTypeEquivalence(GraphQLObjectType objectTyoe, GraphQLInterfaceType interfaceType, SchemaValidationErrorCollector validationErrorCollector, GraphQLFieldDefinition interfaceFieldDef, GraphQLFieldDefinition objectFieldDef) {
        // the reference implementation has a full but complicated abstract type equivalence check
        // this is not that but we can add that later.  It does cover the major cases however
        String interfaceFieldDefStr = getUnwrappedTypeName(interfaceFieldDef.getType());
        String objectFieldDefStr = getUnwrappedTypeName(objectFieldDef.getType());

        if (!interfaceFieldDefStr.equals(objectFieldDefStr)) {
            validationErrorCollector.addError(
                    error(format("object type '%s' does not implement interface '%s' because field '%s' is defined as '%s' type and not as '%s' type",
                            objectTyoe.getName(), interfaceType.getName(), interfaceFieldDef.getName(), objectFieldDefStr, interfaceFieldDefStr)));
        }
    }

    private SchemaValidationError error(String msg) {
        return new SchemaValidationError(ObjectDoesNotImplementItsInterfaces, msg);
    }

}
