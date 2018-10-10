package graphql.schema;

import graphql.Internal;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionInfo;
import graphql.execution.FieldCollector;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.ValuesResolver;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.FragmentDefinition;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Internal
public class DataFetchingFieldSelectionSetImpl implements DataFetchingFieldSelectionSet {

    private final static DataFetchingFieldSelectionSet NOOP = new DataFetchingFieldSelectionSet() {
        @Override
        public Map<String, List<Field>> get() {
            return emptyMap();
        }

        @Override
        public Map<String, Map<String, Object>> getArguments() {
            return emptyMap();
        }

        @Override
        public Map<String, GraphQLFieldDefinition> getDefinitions() {
            return emptyMap();
        }

        @Override
        public boolean contains(String fieldGlobPattern) {
            return false;
        }

        @Override
        public SelectedField getField(String fieldName) {
            return null;
        }

        @Override
        public List<SelectedField> getFields() {
            return emptyList();
        }

        @Override
        public List<SelectedField> getFields(String fieldGlobPattern) {
            return emptyList();
        }
    };

    public static DataFetchingFieldSelectionSet newCollector(ExecutionContext executionContext, GraphQLType fieldType, List<Field> fields) {
        GraphQLType unwrappedType = ExecutionInfo.unwrapBaseType(fieldType);
        if (unwrappedType instanceof GraphQLFieldsContainer) {
            return new DataFetchingFieldSelectionSetImpl(executionContext, (GraphQLFieldsContainer) unwrappedType, fields);
        } else {
            // we can only collect fields on object types and interfaces.  Scalars, Unions etc... cant be done.
            return NOOP;
        }
    }

    private static GraphQLObjectType asObjectTypeOrNull(GraphQLType unwrappedType) {
        return unwrappedType instanceof GraphQLObjectType ? (GraphQLObjectType) unwrappedType : null;
    }

    private final List<Field> parentFields;
    private final GraphQLSchema graphQLSchema;
    private final GraphQLFieldsContainer parentFieldType;
    private final Map<String, Object> variables;
    private final Map<String, FragmentDefinition> fragmentsByName;

    private Map<String, List<Field>> selectionSetFields;
    private Map<String, GraphQLFieldDefinition> selectionSetFieldDefinitions;
    private Map<String, Map<String, Object>> selectionSetFieldArgs;
    private Set<String> flattenedFields;

    private DataFetchingFieldSelectionSetImpl(ExecutionContext executionContext, GraphQLFieldsContainer parentFieldType, List<Field> parentFields) {
        this(parentFields, parentFieldType, executionContext.getGraphQLSchema(), executionContext.getVariables(), executionContext.getFragmentsByName());
    }

    public DataFetchingFieldSelectionSetImpl(List<Field> parentFields, GraphQLFieldsContainer parentFieldType, GraphQLSchema graphQLSchema, Map<String, Object> variables, Map<String, FragmentDefinition> fragmentsByName) {
        this.parentFields = parentFields;
        this.graphQLSchema = graphQLSchema;
        this.parentFieldType = parentFieldType;
        this.variables = variables;
        this.fragmentsByName = fragmentsByName;
    }

    @Override
    public Map<String, List<Field>> get() {
        // by having a .get() method we get lazy evaluation.
        computeValuesLazily();
        return selectionSetFields;
    }

    @Override
    public Map<String, Map<String, Object>> getArguments() {
        computeValuesLazily();
        return selectionSetFieldArgs;
    }

    @Override
    public Map<String, GraphQLFieldDefinition> getDefinitions() {
        computeValuesLazily();
        return selectionSetFieldDefinitions;
    }

    @Override
    public boolean contains(String fieldGlobPattern) {
        if (fieldGlobPattern == null || fieldGlobPattern.isEmpty()) {
            return false;
        }
        computeValuesLazily();
        PathMatcher globMatcher = globMatcher(fieldGlobPattern);
        for (String flattenedField : flattenedFields) {
            Path path = Paths.get(flattenedField);
            if (globMatcher.matches(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SelectedField getField(String fqFieldName) {
        computeValuesLazily();

        List<Field> fields = selectionSetFields.get(fqFieldName);
        if (fields == null) {
            return null;
        }
        GraphQLFieldDefinition fieldDefinition = selectionSetFieldDefinitions.get(fqFieldName);
        Map<String, Object> arguments = selectionSetFieldArgs.get(fqFieldName);
        arguments = arguments == null ? emptyMap() : arguments;
        return new SelectedFieldImpl(fqFieldName, fields, fieldDefinition, arguments);
    }

    @Override
    public List<SelectedField> getFields(String fieldGlobPattern) {
        if (fieldGlobPattern == null || fieldGlobPattern.isEmpty()) {
            return emptyList();
        }
        computeValuesLazily();

        List<String> targetNames = new ArrayList<>();
        PathMatcher globMatcher = globMatcher(fieldGlobPattern);
        for (String flattenedField : flattenedFields) {
            Path path = Paths.get(flattenedField);
            if (globMatcher.matches(path)) {
                targetNames.add(flattenedField);
            }
        }
        return targetNames.stream()
                .map(this::getField)
                .collect(Collectors.toList());
    }

    @Override
    public List<SelectedField> getFields() {
        computeValuesLazily();

        return flattenedFields.stream()
                .map(this::getField)
                .collect(Collectors.toList());
    }

    private class SelectedFieldImpl implements SelectedField {
        private final String qualifiedName;
        private final String name;
        private final GraphQLFieldDefinition fieldDefinition;
        private final DataFetchingFieldSelectionSet selectionSet;
        private final Map<String, Object> arguments;

        private SelectedFieldImpl(String qualifiedName, List<Field> parentFields, GraphQLFieldDefinition fieldDefinition, Map<String, Object> arguments) {
            this.qualifiedName = qualifiedName;
            this.name = parentFields.get(0).getName();
            this.fieldDefinition = fieldDefinition;
            this.arguments = arguments;
            GraphQLType unwrappedType = ExecutionInfo.unwrapBaseType(fieldDefinition.getType());
            if (unwrappedType instanceof GraphQLFieldsContainer) {
                this.selectionSet = new DataFetchingFieldSelectionSetImpl(parentFields, (GraphQLFieldsContainer) unwrappedType, graphQLSchema, variables, fragmentsByName);
            } else {
                this.selectionSet = NOOP;
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getQualifiedName() {
            return qualifiedName;
        }

        @Override
        public GraphQLFieldDefinition getFieldDefinition() {
            return fieldDefinition;
        }

        @Override
        public Map<String, Object> getArguments() {
            return arguments;
        }

        @Override
        public DataFetchingFieldSelectionSet getSelectionSet() {
            return selectionSet;
        }
    }

    private PathMatcher globMatcher(String fieldGlobPattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + fieldGlobPattern);
    }

    private void computeValuesLazily() {
        synchronized (this) {
            if (selectionSetFields != null) {
                return;
            }

            selectionSetFields = new LinkedHashMap<>();
            selectionSetFieldDefinitions = new LinkedHashMap<>();
            selectionSetFieldArgs = new LinkedHashMap<>();
            flattenedFields = new LinkedHashSet<>();

            traverseFields(parentFields, parentFieldType, "");
        }
    }

    private final static String SEP = "/";


    private void traverseFields(List<Field> fieldList, GraphQLFieldsContainer parentFieldType, String fieldPrefix) {
        FieldCollector fieldCollector = new FieldCollector();
        ValuesResolver valuesResolver = new ValuesResolver();

        FieldCollectorParameters parameters = FieldCollectorParameters.newParameters()
                .schema(graphQLSchema)
                .objectType(asObjectTypeOrNull(parentFieldType))
                .fragments(fragmentsByName)
                .variables(variables)
                .build();

        Map<String, List<Field>> collectedFields = fieldCollector.collectFields(parameters, fieldList);
        for (Map.Entry<String, List<Field>> entry : collectedFields.entrySet()) {
            String fieldName = mkFieldName(fieldPrefix, entry.getKey());
            List<Field> collectedFieldList = entry.getValue();
            selectionSetFields.put(fieldName, collectedFieldList);

            Field field = collectedFieldList.get(0);
            GraphQLFieldDefinition fieldDef = Introspection.getFieldDef(graphQLSchema, parentFieldType, field.getName());
            GraphQLType unwrappedType = ExecutionInfo.unwrapBaseType(fieldDef.getType());
            Map<String, Object> argumentValues = valuesResolver.getArgumentValues(fieldDef.getArguments(), field.getArguments(), variables);

            selectionSetFieldArgs.put(fieldName, argumentValues);
            selectionSetFieldDefinitions.put(fieldName, fieldDef);
            flattenedFields.add(fieldName);

            if (unwrappedType instanceof GraphQLFieldsContainer) {
                traverseFields(collectedFieldList, (GraphQLFieldsContainer) unwrappedType, fieldName);
            }
        }
    }

    private String mkFieldName(String fieldPrefix, String fieldName) {
        return (!fieldPrefix.isEmpty() ? fieldPrefix + SEP : "") + fieldName;
    }
}
