package graphql.execution;

import graphql.GraphQLException;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionContextBuilder {

    private Resolver resolver;

    public ExecutionContextBuilder(Resolver resolver) {
        this.resolver = resolver;
    }

    public ExecutionContext build(GraphQLSchema graphQLSchema, Object root, Document document, String operationName, Map<String, Object> args) {
        Map<String, FragmentDefinition> fragmentsByName = new LinkedHashMap<>();
        Map<String, OperationDefinition> operationsByName = new LinkedHashMap<>();

        for (Definition definition : document.getDefinitions()) {
            if (definition instanceof OperationDefinition) {
                OperationDefinition operationDefinition = (OperationDefinition) definition;
                operationsByName.put(operationDefinition.getName(), operationDefinition);
            }
            if (definition instanceof FragmentDefinition) {
                FragmentDefinition fragmentDefinition = (FragmentDefinition) definition;
                fragmentsByName.put(fragmentDefinition.getName(), fragmentDefinition);
            }
        }
        if (operationName != null && operationsByName.size() > 1) {
            throw new GraphQLException("missing operation name");
        }
        OperationDefinition operation;

        if (operationName == null) {
            operation = operationsByName.values().iterator().next();
        } else {
            operation = operationsByName.get(operationName);
        }
        if (operation == null) {
            throw new GraphQLException();
        }

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setGraphQLSchema(graphQLSchema);
        executionContext.setOperationDefinition(operation);
        executionContext.setRoot(root);
        executionContext.setFragmentsByName(fragmentsByName);
        Map<String, Object> variableValues = resolver.getVariableValues(graphQLSchema, operation.getVariableDefinitions(), args);
        executionContext.setVariables(variableValues);
        return executionContext;
    }
}
