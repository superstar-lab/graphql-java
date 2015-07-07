package graphql.execution;


import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionContext {

    private GraphQLSchema graphQLSchema;
    private Map<String, FragmentDefinition> fragmentsByName = new LinkedHashMap<>();
    private OperationDefinition operationDefinition;
    private Map<String, Object> variables = new LinkedHashMap<>();
    private Object root;


    public GraphQLSchema getGraphQLSchema() {
        return graphQLSchema;
    }

    public void setGraphQLSchema(GraphQLSchema graphQLSchema) {
        this.graphQLSchema = graphQLSchema;
    }

    public Map<String, FragmentDefinition> getFragmentsByName() {
        return fragmentsByName;
    }

    public void setFragmentsByName(Map<String, FragmentDefinition> fragmentsByName) {
        this.fragmentsByName = fragmentsByName;
    }

    public OperationDefinition getOperationDefinition() {
        return operationDefinition;
    }

    public void setOperationDefinition(OperationDefinition operationDefinition) {
        this.operationDefinition = operationDefinition;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Object getRoot() {
        return root;
    }

    public void setRoot(Object root) {
        this.root = root;
    }

    public FragmentDefinition getFragment(String name) {
        return fragmentsByName.get(name);
    }
}
