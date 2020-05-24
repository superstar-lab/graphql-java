package graphql.execution.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A reactive Publisher that bridges over another Publisher of `D` and maps the results
 * to type `U` via a CompletionStage, handling errors in that stage
 *
 * @param <D> the down stream type
 * @param <U> the up stream type to be mapped to
 */
public class CompletionStageMappingPublisher<D, U> implements Publisher<D> {
    private final Publisher<U> upstreamPublisher;
    private final Function<U, CompletionStage<D>> mapper;

    /**
     * You need the following :
     *
     * @param upstreamPublisher an upstream source of data
     * @param mapper            a mapper function that turns upstream data into a promise of mapped D downstream data
     */
    public CompletionStageMappingPublisher(Publisher<U> upstreamPublisher, Function<U, CompletionStage<D>> mapper) {
        this.upstreamPublisher = upstreamPublisher;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super D> downstreamSubscriber) {
        upstreamPublisher.subscribe(new Subscriber<U>() {
            Subscription delegatingSubscription;
            final Queue<CompletionStage<?>> inFlightDataQ = new ArrayDeque<>();
            final AtomicReference<Runnable> onCompleteOrErrorRun = new AtomicReference<>();
            final AtomicBoolean onCompleteOrErrorRunCalled = new AtomicBoolean(false);


            @Override
            public void onSubscribe(Subscription subscription) {
                delegatingSubscription = new DelegatingSubscription(subscription);
                downstreamSubscriber.onSubscribe(delegatingSubscription);
            }

            @Override
            public void onNext(U u) {
                // for safety - no more data after we have called done/error - we should not get this BUT belts and braces
                if (onCompleteOrErrorRunCalled.get()) {
                    return;
                }
                try {
                    CompletionStage<D> completionStage = mapper.apply(u);
                    offerToInFlightQ(completionStage);
                    completionStage.whenComplete(whenNextFinished(completionStage));
                } catch (RuntimeException throwable) {
                    handleThrowable(throwable);
                }
            }

            private BiConsumer<D, Throwable> whenNextFinished(CompletionStage<D> completionStage) {
                return (d, throwable) -> {
                    try {
                        if (throwable != null) {
                            handleThrowable(throwable);
                        } else {
                            downstreamSubscriber.onNext(d);
                        }
                    } finally {
                        Runnable runOnCompleteOrErrorRun = onCompleteOrErrorRun.get();
                        boolean empty = removeFromInFlightQAndCheckIfEmpty(completionStage);
                        if (empty && runOnCompleteOrErrorRun != null) {
                            onCompleteOrErrorRun.set(null);
                            runOnCompleteOrErrorRun.run();
                        }
                    }
                };
            }

            private void handleThrowable(Throwable throwable) {
                downstreamSubscriber.onError(throwable);
                //
                // reactive semantics say that IF an exception happens on a publisher
                // then onError is called and no more messages flow.  But since the exception happened
                // during the mapping, the upstream publisher does not no about this.
                // so we cancel to bring the semantics back together, that is as soon as an exception
                // has happened, no more messages flow
                //
                delegatingSubscription.cancel();
            }

            @Override
            public void onError(Throwable t) {
                onCompleteOrError(() -> {
                    onCompleteOrErrorRunCalled.set(true);
                    downstreamSubscriber.onError(t);
                });
            }

            @Override
            public void onComplete() {
                onCompleteOrError(() -> {
                    onCompleteOrErrorRunCalled.set(true);
                    downstreamSubscriber.onComplete();
                });
            }

            private void onCompleteOrError(Runnable doneCodeToRun) {
                if (inFlightQIsEmpty()) {
                    // run right now
                    doneCodeToRun.run();
                } else {
                    onCompleteOrErrorRun.set(doneCodeToRun);
                }
            }

            private void offerToInFlightQ(CompletionStage<?> completionStage) {
                synchronized (inFlightDataQ) {
                    inFlightDataQ.offer(completionStage);
                }
            }

            private boolean removeFromInFlightQAndCheckIfEmpty(CompletionStage<?> completionStage) {
                // uncontested locks in java are cheap - we dont expect much contention here
                synchronized (inFlightDataQ) {
                    inFlightDataQ.remove(completionStage);
                    return inFlightDataQ.isEmpty();
                }
            }

            private boolean inFlightQIsEmpty() {
                synchronized (inFlightDataQ) {
                    return inFlightDataQ.isEmpty();
                }
            }
        });
    }
}
