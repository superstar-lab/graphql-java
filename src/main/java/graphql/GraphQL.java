package graphql;


import graphql.execution.Execution;
import graphql.execution.ExecutionResult;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;
import graphql.validation.ValidationErrorType;
import graphql.validation.Validator;
import org.antlr.v4.runtime.RecognitionException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static graphql.Assert.assertNotNull;

public class GraphQL {


    private final GraphQLSchema graphQLSchema;
    private final ExecutorService executorService;

    public GraphQL(GraphQLSchema graphQLSchema) {
        this(graphQLSchema, Executors.newCachedThreadPool());
    }


    public GraphQL(GraphQLSchema graphQLSchema, ExecutorService executorService) {
        this.graphQLSchema = graphQLSchema;
        this.executorService = executorService;
    }

    public ExecutionResult execute(String requestString) {
        return execute(requestString, null);
    }

    public ExecutionResult execute(String requestString, Object context) {
        return execute(requestString, context, Collections.<String, Object>emptyMap());
    }

    public ExecutionResult execute(String requestString, Object context, Map<String, Object> arguments) {
        assertNotNull(arguments, "arguments can't be null");
        Parser parser = new Parser();
        Document document;
        try {
            document = parser.parseDocument(requestString);
        } catch (RecognitionException e) {
            ValidationError validationError = new ValidationError(ValidationErrorType.InvalidSyntax);
            return new ExecutionResult(Arrays.asList(validationError));
        }
        Execution execution = new Execution(executorService);
        Validator validator = new Validator();
        List<ValidationError> validationErrors = validator.validateDocument(graphQLSchema, document);
        if (validationErrors.size() > 0) {
            ExecutionResult result = new ExecutionResult(validationErrors);
            return result;
        }
        return execution.execute(graphQLSchema, context, document, null, arguments);
    }


}
