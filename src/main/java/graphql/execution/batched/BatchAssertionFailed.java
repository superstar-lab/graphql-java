package graphql.execution.batched;

import graphql.GraphQLException;
import graphql.PublicApi;


@Deprecated
@PublicApi
public class BatchAssertionFailed extends GraphQLException {
    public BatchAssertionFailed() {
        super();
    }

    public BatchAssertionFailed(String message) {
        super(message);
    }

    public BatchAssertionFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public BatchAssertionFailed(Throwable cause) {
        super(cause);
    }
}
