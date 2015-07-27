package graphql.validation.rules;


import graphql.execution.TypeFromAST;
import graphql.language.*;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.validation.AbstractRule;
import graphql.validation.ErrorFactory;
import graphql.validation.ValidationContext;
import graphql.validation.ValidationErrorCollector;

import java.util.*;

import static graphql.validation.ValidationErrorType.FieldsConflict;

public class OverlappingFieldsCanBeMerged extends AbstractRule {

    ErrorFactory errorFactory = new ErrorFactory();

    public OverlappingFieldsCanBeMerged(ValidationContext validationContext, ValidationErrorCollector validationErrorCollector) {
        super(validationContext, validationErrorCollector);
    }

    @Override
    public void checkSelectionSet(SelectionSet selectionSet) {
        Map<String, List<FieldAndType>> fieldMap = new LinkedHashMap<>();
        Set<String> visitedFragmentSpreads = new LinkedHashSet<>();
        collectFields(fieldMap, selectionSet, getValidationContext().getOutputType(), visitedFragmentSpreads);
        List<Conflict> conflicts = findConflicts(fieldMap);
        for (Conflict conflict : conflicts) {
            addError(errorFactory.newError(FieldsConflict, conflict.fields, conflict.reason));
        }

    }

    private List<Conflict> findConflicts(Map<String, List<FieldAndType>> fieldMap) {
        List<Conflict> result = new ArrayList<>();
        for (String name : fieldMap.keySet()) {
            List<FieldAndType> fieldAndTypes = fieldMap.get(name);
            for (int i = 0; i < fieldAndTypes.size(); i++) {
                for (int j = i + i; j < fieldAndTypes.size(); j++) {
                    Conflict conflict = findConflict(name, fieldAndTypes.get(i), fieldAndTypes.get(j));
                    if (conflict != null) {
                        result.add(conflict);
                    }
                }
            }
        }
        return result;
    }

    private Conflict findConflict(String responseName, FieldAndType fieldAndType1, FieldAndType fieldAndType2) {

        Field field1 = fieldAndType1.field;
        Field field2 = fieldAndType2.field;

        GraphQLType type1 = fieldAndType1.graphQLType;
        GraphQLType type2 = fieldAndType2.graphQLType;

        String fieldName1 = field1.getName();
        String fieldName2 = field2.getName();
        if (!fieldName1.equals(fieldName2)) {
            String reason = String.format("%s: %s and %s are different fields", responseName, fieldName1, fieldName2);
            return new Conflict(responseName, reason, field1, field2);
        }

        if (!sameType(type1, type2)) {
            String reason = String.format("%s: they return differing types %s and %s", responseName, type1.getName(), type2.getName());
            return new Conflict(responseName, reason, field1, field2);
        }


        if (!sameArguments(field1.getArguments(), field2.getArguments())) {
            String reason = String.format("%s: they have differing arguments", responseName);
            return new Conflict(responseName, reason, field1, field2);
        }
        if (!sameDirectives(field1.getDirectives(), field2.getDirectives())) {
            String reason = String.format("%s: they have differing directives", responseName);
            return new Conflict(responseName, reason, field1, field2);
        }
        SelectionSet selectionSet1 = field1.getSelectionSet();
        SelectionSet selectionSet2 = field2.getSelectionSet();
        if (selectionSet1 != null && selectionSet2 != null) {
            Set<String> visitedFragmentSpreads = new LinkedHashSet<>();
            Map<String, List<FieldAndType>> subfieldMap = new LinkedHashMap<>();
            collectFields(subfieldMap, selectionSet1, type1, visitedFragmentSpreads);
            collectFields(subfieldMap, selectionSet2, type2, visitedFragmentSpreads);
            List<Conflict> subConflicts = findConflicts(subfieldMap);
            if (subConflicts.size() > 0) {
                for(Conflict conflict : subConflicts){

                }
            }
        }

        return null;

    }

    private boolean sameType(GraphQLType type1, GraphQLType type2) {
        if (type1 == null && type2 == null) return true;
        if (type1 == null) return false;
        if (type2 == null) return false;
        return type1.equals(type2);
    }

    private boolean sameValue(Value value1, Value value2) {
        if (value1 == null && value2 == null) return true;
        if (value1 == null) return false;
        if (value2 == null) return false;
        return value1.equals(value2);
    }

    private boolean sameArguments(List<Argument> arguments1, List<Argument> arguments2) {
        if (arguments1.size() != arguments2.size()) return false;
        for (Argument argument : arguments1) {
            Argument matchedArgument = findArgumentByName(argument.getName(), arguments2);
            if (matchedArgument == null) return false;
            if (!sameValue(argument.getValue(), matchedArgument.getValue())) return false;
        }
        return true;
    }

    private Argument findArgumentByName(String name, List<Argument> arguments) {
        for (Argument argument : arguments) {
            if (argument.getName().equals(name)) return argument;
        }
        return null;
    }

    private boolean sameDirectives(List<Directive> directives1, List<Directive> directives2) {
        if (directives1.size() != directives2.size()) return false;
        for (Directive directive : directives1) {
            Directive matchedDirective = findDirectiveByName(directive.getName(), directives2);
            if (matchedDirective == null) return false;
            if (!sameArguments(directive.getArguments(), matchedDirective.getArguments())) return false;
        }
        return true;
    }

    private Directive findDirectiveByName(String name, List<Directive> directives) {
        for (Directive directive : directives) {
            if (directive.getName().equals(name)) {
                return directive;
            }
        }
        return null;
    }


    private void collectFields(Map<String, List<FieldAndType>> fieldMap, SelectionSet selectionSet, GraphQLType parentType, Set<String> visitedFragmentSpreads) {

        for (Selection selection : selectionSet.getSelections()) {
            if (selection instanceof Field) {
                Field field = (Field) selection;
                String responseName = field.getAlias() != null ? field.getAlias() : field.getName();
                if (!fieldMap.containsKey(responseName)) {
                    fieldMap.put(responseName, new ArrayList<FieldAndType>());
                }
                GraphQLOutputType fieldType = null;
                if (parentType instanceof GraphQLFieldsContainer) {
                    GraphQLFieldsContainer fieldsContainer = (GraphQLFieldsContainer) parentType;
                    GraphQLFieldDefinition fieldDefinition = fieldsContainer.getFieldDefinition(((Field) selection).getName());
                    fieldType = fieldDefinition != null ? fieldDefinition.getType() : null;
                }
                fieldMap.get(responseName).add(new FieldAndType(field, fieldType));

            } else if (selection instanceof InlineFragment) {
                InlineFragment inlineFragment = (InlineFragment) selection;
                GraphQLOutputType graphQLType = (GraphQLOutputType) TypeFromAST.getTypeFromAST(getValidationContext().getSchema(),
                        inlineFragment.getTypeCondition());
                collectFields(fieldMap, inlineFragment.getSelectionSet(), graphQLType, visitedFragmentSpreads);

            } else if (selection instanceof FragmentSpread) {
                FragmentSpread fragmentSpread = (FragmentSpread) selection;
                FragmentDefinition fragment = getValidationContext().getFragment(fragmentSpread.getName());
                if (fragment == null) continue;
                if (visitedFragmentSpreads.contains(fragment.getName())) {
                    continue;
                }
                visitedFragmentSpreads.add(fragment.getName());
                GraphQLOutputType graphQLType = (GraphQLOutputType) TypeFromAST.getTypeFromAST(getValidationContext().getSchema(),
                        fragment.getTypeCondition());
                collectFields(fieldMap, fragment.getSelectionSet(), graphQLType, visitedFragmentSpreads);
            }
        }

    }

    private static class Conflict {
        String responseName;
        String reason;
        List<Field> fields = new ArrayList<>();

        public Conflict(String responseName, String reason, Field field1, Field field2) {
            this.responseName = responseName;
            this.reason = reason;
            this.fields.add(field1);
            this.fields.add(field2);
        }

    }


    private static class FieldAndType {
        public FieldAndType(Field field, GraphQLType graphQLType) {
            this.field = field;
            this.graphQLType = graphQLType;
        }

        Field field;
        GraphQLType graphQLType;
    }
}
