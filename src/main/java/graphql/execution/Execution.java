package graphql.execution;


import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLException;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.parameters.DataFetchParameters;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.execution.ExecutionParameters.newParameters;
import static graphql.execution.TypeInfo.newTypeInfo;

public class Execution {

    private final FieldCollector fieldCollector = new FieldCollector();
    private final ExecutionStrategy queryStrategy;
    private final ExecutionStrategy mutationStrategy;
    private final Instrumentation instrumentation;

    public Execution(ExecutionStrategy queryStrategy, ExecutionStrategy mutationStrategy, Instrumentation instrumentation) {
        this.queryStrategy = queryStrategy != null ? queryStrategy : new SimpleExecutionStrategy();
        this.mutationStrategy = mutationStrategy != null ? mutationStrategy : new SimpleExecutionStrategy();
        this.instrumentation = instrumentation;
    }

    public ExecutionResult execute(ExecutionId executionId, GraphQLSchema graphQLSchema, Object root, Document document, String operationName, Map<String, Object> args) {
        ExecutionContextBuilder executionContextBuilder = new ExecutionContextBuilder(new ValuesResolver(), instrumentation);
        ExecutionContext executionContext = executionContextBuilder
                .executionId(executionId)
                .build(graphQLSchema, queryStrategy, mutationStrategy, root, document, operationName, args);
        return executeOperation(executionContext, root, executionContext.getOperationDefinition());
    }

    private GraphQLObjectType getOperationRootType(GraphQLSchema graphQLSchema, OperationDefinition operationDefinition) {
        if (operationDefinition.getOperation() == OperationDefinition.Operation.MUTATION) {
            return graphQLSchema.getMutationType();

        } else if (operationDefinition.getOperation() == OperationDefinition.Operation.QUERY) {
            return graphQLSchema.getQueryType();

        } else {
            throw new GraphQLException();
        }
    }

    private ExecutionResult executeOperation(
            ExecutionContext executionContext,
            Object root,
            OperationDefinition operationDefinition) {

        InstrumentationContext<ExecutionResult> dataFetchCtx = instrumentation.beginDataFetch(new DataFetchParameters(executionContext));

        GraphQLObjectType operationRootType = getOperationRootType(executionContext.getGraphQLSchema(), operationDefinition);

        Map<String, List<Field>> fields = new LinkedHashMap<String, List<Field>>();
        fieldCollector.collectFields(executionContext, operationRootType, operationDefinition.getSelectionSet(), new ArrayList<String>(), fields);


        ExecutionParameters parameters = newParameters()
                .typeInfo(newTypeInfo().type(operationRootType))
                .source(root)
                .fields(fields)
                .build();

        ExecutionResult result;
        try {
            if (operationDefinition.getOperation() == OperationDefinition.Operation.MUTATION) {
                result = mutationStrategy.execute(executionContext, parameters);
            } else {
                result = queryStrategy.execute(executionContext, parameters);
            }
        } catch (NonNullableFieldWasNullException e) {
            // this means it was non null types all the way from an offending non null type
            // up to the root object type and there was a a null value some where.
            //
            // The spec says we should return null for the data in this case
            //
            // http://facebook.github.io/graphql/#sec-Errors-and-Non-Nullability
            //
            result = new ExecutionResultImpl(null, executionContext.getErrors());
        }

        dataFetchCtx.onEnd(result);
        return result;
    }
}
