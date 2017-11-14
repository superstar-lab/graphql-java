��    W      �              �  �   �    g  $   v  �   �  r   ?  i   �  �   	     �	     �	     
     )
  �   B
  �   �
  �   a  �   ]     .    <  �   V     �  	              /  �   P  �   �  �   �  ,   v  �   �  /  Z  �   �  '  �  �   �  �   a  �   �  �   �  �   <  �   2       �   -  	   �  �   �  �   t  �     �     [   �     2     :  \   H  v   �          8  {  P     �      �   �   !  �   �!  �  �"  i   )$  j   �$  |   �$  �   {%     &  y   !&  �   �&  �   C'  *   �'  ?   
(  `   J(  �   �(  �   m)  �   \*  �   �*  O   �+  v   �+  �   N,  �   �,  ~   |-  �   �-  �   �.  |   6/  V   �/  x   
0  :   �0  �   �0  �   �1  p   b2     �2  �  �2  �   i4    C5  $   R6  �   w6  r   7  i   �7  �   �7     �8     �8     �8     9  �   9  �   �9  �   =:  �   9;     
<    <  �   2=     �=  	   �=     �=      >  �   ,>  �   �>  �   �?  ,   R@  �   @  /  6A  �   fB  '  bC  �   �D  �   =E  �   �E  �   �F  �   G  �   H     �H  �   	I  	   �I  �   �I  �   PJ  �   �J  �   �K  [   �L     M     M  \   $M  v   �M     �M     N  {  ,N     �O     �O  �   �O  �   �P  �  _Q  i   S  j   oS  |   �S  �   WT     �T  y   �T  �   wU  �   V  *   �V  ?   �V  `   &W  �   �W  �   IX  �   8Y  �   �Y  O   cZ  v   �Z  �   *[  �   �[  ~   X\  �   �\  �   ]  |   ^  V   �^  x   �^  :   __  �   �_  �   �`  p   >a     �a   A class derived from ``graphql.execution.ExecutionStrategy`` is used to run a query or mutation.  A number of different strategies are provided with graphql-java and if you are really keen you can even write your own. A common way to do that is use a JSON serialisation library like Jackson or GSON.  However exactly how they interpret the data result is particular to them.  For example `nulls` are important in graphql results and hence you must set up the json mappers to include them. A data fetcher might look like this: A good starting point to learn more about mutating data in graphql is `http://graphql.org/learn/queries/#mutations <http://graphql.org/learn/queries/#mutations>`_. A simple `graphql.schema.visibility.BlockedFields` implementation based on fully qualified field name is provided. Also see the page on subscriptions for more details on how to write a subscription based graphql service. Alternatively, schemas with nested lists may benefit from using a ``graphql.execution.batched.BatchedExecutionStrategy`` and creating batched DataFetchers with get() methods annotated @Batched. AsyncExecutionStrategy AsyncSerialExecutionStrategy Asynchronous Execution BatchedExecutionStrategy Before the ``graphql-java`` engine executes a query it must be parsed and validated, and this process can be somewhat time consuming. By default every fields defined in a `GraphqlSchema` is available.  There are cases where you may want to restrict certain fields depending on the user. By default the "query" execution strategy is ``graphql.execution.AsyncExecutionStrategy`` which will dispatch each field as ``CompleteableFuture`` objects and not care which ones complete first.  This strategy allows for the most performant execution. Create an instance of preferred cache instance, here is `Caffeine <https://github.com/ben-manes/caffeine>`_  used as it is a high quality caching solution. The cache instance should be thread safe and shared. Data Fetchers Each ``DataFetcher`` is passed a ``graphql.schema.DataFetchingEnvironment`` object which contains what field is being fetched, what arguments have been supplied to the field and other information such as the field's parent object, the query root object or the query context object. Each graphql field type has a ``graphql.schema.DataFetcher`` associated with it.  Other graphql implementations often call this type of code *resolvers**. Exceptions while fetching data Execution Execution Strategies ExecutorServiceExecutionStrategy For example imagine your data fetcher threw this exception.  The `foo` and `fizz` attributes would be included in the resultant graphql error. For example the code above records the underlying exception and stack trace.  Some people may prefer not to see that in the output error list.  So you can use this mechanism to change that behaviour. Graphql subscriptions allows you to create stateful subscriptions to graphql data.  You uses ``SubscriptionExecutionStrategy`` as your execution strategy as it has the support for the reactive-streams APIs. Here is the code for the standard behaviour. However you will need to fetch your top level domain objects via your own custom data fetchers.  This might involve making a database call or contacting another system over HTTP say. If a ``graphql.schema.DataFetcher`` returns a ``CompletableFuture<T>`` object then this will be composed into the overall asynchronous query execution.  This means you can fire off a number of field fetching requests in parallel.  Exactly what threading strategy you use is up to your data fetcher code. If an exception happens during the data fetcher call, then the execution strategy by default will make a ``graphql.ExceptionWhileDataFetching`` error and add it to the list of errors on the result.  Remember graphql allows partial results with errors. If the exception you throw is itself a `GraphqlError` then it will transfer the message and custom extensions attributes from that exception into the `ExceptionWhileDataFetching` object.  This allows you to place your own custom attributes into the graphql error that is sent back to the caller. In essence you need to define a ``GraphQLObjectType`` that takes arguments as input.  Those arguments are what you can use to mutate your data store via the data fetcher invoked. In fact the code above is equivalent to the default settings and is a very sensible choice of execution strategies for most cases. In fact under the covers, the graphql-java engine uses asynchronous execution and makes the ``.execute()`` method appear synchronous by calling join for you.  So the following code is in fact the same. In order to achieve high cache hit ration it is recommended that field arguments are passed in as variables instead of directly in the query. In the above example, the execution will wait for the data fetcher to return before moving on.  You can make execution of the ``DataFetcher`` asynchronous by returning a ``CompletionStage`` to data, that is explained more further down this page. It will however assemble the results in order.  The query result will follow the graphql specification and return object values assembled in query field order.  Only the execution of data fetching is free to be in any order. Limiting Field Visibility More complex query examples can be found in the `StarWars query tests <https://github.com/graphql-java/graphql-java/blob/master/src/test/groovy/graphql/StarWarsQueryTest.groovy>`_ Mutations Note that this puts your server in contravention of the graphql specification and expectations of most clients so use this with caution. Notice how it calls a data store to mutate the backing database and then returns a ``Review`` object that can be used as the output values to the caller. Notice that the input arguments are of type ``GraphQLInputObjectType``.  This is important.  Input arguments can ONLY be of that type and you cannot use output types such as ``GraphQLObjectType``.  Scalars types are consider both input and output types. Often you can rely on ``graphql.schema.PropertyDataFetcher`` to examine Java POJO objects to provide field values from them.  If your don't specify a data fetcher on a field, this is what will be used. Please note that this does not cache the result of the query, only the parsed ``Document``. Queries Query Caching See `specification <http://facebook.github.io/graphql/#sec-Normal-evaluation>`_ for details. See http://www.reactive-streams.org/ for more information on the reactive ``Publisher`` and ``Subscriber`` interfaces. Serializing results to JSON Should be rewritten as: So ``graphql.execution.AsyncSerialExecutionStrategy`` is used by default for mutations and will ensure that each field is completed before it processes the next one and so forth.  You can still return ``CompletionStage`` objects in the mutation data fetchers, however they will be executed serially and will be completed before the next mutation field data fetcher is dispatched. So imagine a query as follows SubscriptionExecutionStrategy The ``AsyncExecutionStrategy`` is free to dispatch the *enemies* field at the same time as the *friends* field.  It does not have to do *enemies* first followed by *friends*, which would be less efficient. The ``PreparsedDocumentProvider`` is a functional interface with only a get method and we can therefore pass a method reference that matches the signature into the builder. The ``graphql.execution.ExecutorServiceExecutionStrategy`` execution strategy will always dispatch each field fetch in an asynchronous manner, using the executor you give it.  It differs from ``AsyncExecutionStrategy`` in that it does not rely on the data fetchers to be asynchronous but rather makes the field fetch invocation asynchronous by submitting each field to the provided `java.util.concurrent.ExecutorService`. The code above is written in long form.  With Java 8 lambdas it can be written more succinctly as follows The data fetcher here is responsible for executing the mutation and returning some sensible output values. The data fetchers invoked can themselves return `CompletionStage`` values and this will create fully asynchronous behaviour. The following code uses the standard Java ``java.util.concurrent.ForkJoinPool.commonPool()`` thread executor to supply values in another thread. The following query: The graphql specification says that mutations MUST be executed serially and in the order in which the query fields occur. The graphql-java engine ensures that all the ``CompletableFuture`` objects are composed together to provide an execution result that follows the graphql specification. The most common way to call graphql is over HTTP and to expect a JSON response back.  So you need to turn an `graphql.ExecutionResult` into a JSON payload. The mutation is invoked via a query like : The query is now reused regardless of variable values provided. The result of a query is an ``ExecutionResult`` which is the query data and/or a list of errors. The use of ``CompletableFuture`` allows you to compose actions and functions that will be applied when the execution completes.  The final call to ``.join()`` waits for the execution to happen. There is a helpful shortcut in graphql-java to create asynchronous data fetchers. Use ``graphql.schema.AsyncDataFetcher.async(DataFetcher<T>)`` to wrap a ``DataFetcher``. This can be used with static imports to produce more readable code. There is also another implementation that prevents instrumentation from being able to be performed on your schema, if that is a requirement. This behaviour is allowed in the graphql specification and in fact is actively encouraged http://facebook.github.io/graphql/#sec-Query for read only queries. This behaviour makes it unsuitable to be used as a mutation execution strategy. This will ensure that the result follows the specification outlined in http://facebook.github.io/graphql/#sec-Response To avoid the need for re-parse/validate the ``GraphQL.Builder`` allows an instance of ``PreparsedDocumentProvider`` to reuse ``Document`` instances. To ensure you get a JSON result that confirms 100% to the graphql spec, you should call `toSpecification` on the result and then send that back as JSON. To execute a query against a schema build a new ``GraphQL`` object with the appropriate arguments and then call ``execute()``. You can change this behaviour by creating your own ``graphql.execution.DataFetcherExceptionHandler`` exception handling code and giving that to the execution strategy. You can create your own derivation of `GraphqlFieldVisibility` to check what ever you need to do to work out what fields should be visible or not. You can do this by using a `graphql.schema.visibility.GraphqlFieldVisibility` implementation and attaching it to the schema. You can wire in what execution strategy to use when you create the ``GraphQL`` object. You need to send in arguments during that mutation operation, in this case for the variables for ``$ep`` and ``$review`` You would create types like this to handle this mutation : ``graphql-java`` is not opinionated about how you get your domain data objects, that is very much your concern.  It is also not opinionated on user authorisation to that data.  You should push all that logic into your business logic layer code. graphql-java uses fully asynchronous execution techniques when it executes queries.  You can get the ``CompleteableFuture`` to results by calling ``executeAsync()`` like this on how BatchedExecutionStrategy works here.  Its a pretty special case that I don't know how to explain properly with variables: Project-Id-Version: graphql-java current
Report-Msgid-Bugs-To: 
POT-Creation-Date: 2017-11-11 19:21+0800
PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE
Last-Translator: FULL NAME <EMAIL@ADDRESS>
Language: es
Language-Team: es <LL@li.org>
Plural-Forms: nplurals=2; plural=(n != 1)
MIME-Version: 1.0
Content-Type: text/plain; charset=utf-8
Content-Transfer-Encoding: 8bit
Generated-By: Babel 2.5.1
 A class derived from ``graphql.execution.ExecutionStrategy`` is used to run a query or mutation.  A number of different strategies are provided with graphql-java and if you are really keen you can even write your own. A common way to do that is use a JSON serialisation library like Jackson or GSON.  However exactly how they interpret the data result is particular to them.  For example `nulls` are important in graphql results and hence you must set up the json mappers to include them. A data fetcher might look like this: A good starting point to learn more about mutating data in graphql is `http://graphql.org/learn/queries/#mutations <http://graphql.org/learn/queries/#mutations>`_. A simple `graphql.schema.visibility.BlockedFields` implementation based on fully qualified field name is provided. Also see the page on subscriptions for more details on how to write a subscription based graphql service. Alternatively, schemas with nested lists may benefit from using a ``graphql.execution.batched.BatchedExecutionStrategy`` and creating batched DataFetchers with get() methods annotated @Batched. AsyncExecutionStrategy AsyncSerialExecutionStrategy Asynchronous Execution BatchedExecutionStrategy Before the ``graphql-java`` engine executes a query it must be parsed and validated, and this process can be somewhat time consuming. By default every fields defined in a `GraphqlSchema` is available.  There are cases where you may want to restrict certain fields depending on the user. By default the "query" execution strategy is ``graphql.execution.AsyncExecutionStrategy`` which will dispatch each field as ``CompleteableFuture`` objects and not care which ones complete first.  This strategy allows for the most performant execution. Create an instance of preferred cache instance, here is `Caffeine <https://github.com/ben-manes/caffeine>`_  used as it is a high quality caching solution. The cache instance should be thread safe and shared. Data Fetchers Each ``DataFetcher`` is passed a ``graphql.schema.DataFetchingEnvironment`` object which contains what field is being fetched, what arguments have been supplied to the field and other information such as the field's parent object, the query root object or the query context object. Each graphql field type has a ``graphql.schema.DataFetcher`` associated with it.  Other graphql implementations often call this type of code *resolvers**. Exceptions while fetching data Execution Execution Strategies ExecutorServiceExecutionStrategy For example imagine your data fetcher threw this exception.  The `foo` and `fizz` attributes would be included in the resultant graphql error. For example the code above records the underlying exception and stack trace.  Some people may prefer not to see that in the output error list.  So you can use this mechanism to change that behaviour. Graphql subscriptions allows you to create stateful subscriptions to graphql data.  You uses ``SubscriptionExecutionStrategy`` as your execution strategy as it has the support for the reactive-streams APIs. Here is the code for the standard behaviour. However you will need to fetch your top level domain objects via your own custom data fetchers.  This might involve making a database call or contacting another system over HTTP say. If a ``graphql.schema.DataFetcher`` returns a ``CompletableFuture<T>`` object then this will be composed into the overall asynchronous query execution.  This means you can fire off a number of field fetching requests in parallel.  Exactly what threading strategy you use is up to your data fetcher code. If an exception happens during the data fetcher call, then the execution strategy by default will make a ``graphql.ExceptionWhileDataFetching`` error and add it to the list of errors on the result.  Remember graphql allows partial results with errors. If the exception you throw is itself a `GraphqlError` then it will transfer the message and custom extensions attributes from that exception into the `ExceptionWhileDataFetching` object.  This allows you to place your own custom attributes into the graphql error that is sent back to the caller. In essence you need to define a ``GraphQLObjectType`` that takes arguments as input.  Those arguments are what you can use to mutate your data store via the data fetcher invoked. In fact the code above is equivalent to the default settings and is a very sensible choice of execution strategies for most cases. In fact under the covers, the graphql-java engine uses asynchronous execution and makes the ``.execute()`` method appear synchronous by calling join for you.  So the following code is in fact the same. In order to achieve high cache hit ration it is recommended that field arguments are passed in as variables instead of directly in the query. In the above example, the execution will wait for the data fetcher to return before moving on.  You can make execution of the ``DataFetcher`` asynchronous by returning a ``CompletionStage`` to data, that is explained more further down this page. It will however assemble the results in order.  The query result will follow the graphql specification and return object values assembled in query field order.  Only the execution of data fetching is free to be in any order. Limiting Field Visibility More complex query examples can be found in the `StarWars query tests <https://github.com/graphql-java/graphql-java/blob/master/src/test/groovy/graphql/StarWarsQueryTest.groovy>`_ Mutations Note that this puts your server in contravention of the graphql specification and expectations of most clients so use this with caution. Notice how it calls a data store to mutate the backing database and then returns a ``Review`` object that can be used as the output values to the caller. Notice that the input arguments are of type ``GraphQLInputObjectType``.  This is important.  Input arguments can ONLY be of that type and you cannot use output types such as ``GraphQLObjectType``.  Scalars types are consider both input and output types. Often you can rely on ``graphql.schema.PropertyDataFetcher`` to examine Java POJO objects to provide field values from them.  If your don't specify a data fetcher on a field, this is what will be used. Please note that this does not cache the result of the query, only the parsed ``Document``. Queries Query Caching See `specification <http://facebook.github.io/graphql/#sec-Normal-evaluation>`_ for details. See http://www.reactive-streams.org/ for more information on the reactive ``Publisher`` and ``Subscriber`` interfaces. Serializing results to JSON Should be rewritten as: So ``graphql.execution.AsyncSerialExecutionStrategy`` is used by default for mutations and will ensure that each field is completed before it processes the next one and so forth.  You can still return ``CompletionStage`` objects in the mutation data fetchers, however they will be executed serially and will be completed before the next mutation field data fetcher is dispatched. So imagine a query as follows SubscriptionExecutionStrategy The ``AsyncExecutionStrategy`` is free to dispatch the *enemies* field at the same time as the *friends* field.  It does not have to do *enemies* first followed by *friends*, which would be less efficient. The ``PreparsedDocumentProvider`` is a functional interface with only a get method and we can therefore pass a method reference that matches the signature into the builder. The ``graphql.execution.ExecutorServiceExecutionStrategy`` execution strategy will always dispatch each field fetch in an asynchronous manner, using the executor you give it.  It differs from ``AsyncExecutionStrategy`` in that it does not rely on the data fetchers to be asynchronous but rather makes the field fetch invocation asynchronous by submitting each field to the provided `java.util.concurrent.ExecutorService`. The code above is written in long form.  With Java 8 lambdas it can be written more succinctly as follows The data fetcher here is responsible for executing the mutation and returning some sensible output values. The data fetchers invoked can themselves return `CompletionStage`` values and this will create fully asynchronous behaviour. The following code uses the standard Java ``java.util.concurrent.ForkJoinPool.commonPool()`` thread executor to supply values in another thread. The following query: The graphql specification says that mutations MUST be executed serially and in the order in which the query fields occur. The graphql-java engine ensures that all the ``CompletableFuture`` objects are composed together to provide an execution result that follows the graphql specification. The most common way to call graphql is over HTTP and to expect a JSON response back.  So you need to turn an `graphql.ExecutionResult` into a JSON payload. The mutation is invoked via a query like : The query is now reused regardless of variable values provided. The result of a query is an ``ExecutionResult`` which is the query data and/or a list of errors. The use of ``CompletableFuture`` allows you to compose actions and functions that will be applied when the execution completes.  The final call to ``.join()`` waits for the execution to happen. There is a helpful shortcut in graphql-java to create asynchronous data fetchers. Use ``graphql.schema.AsyncDataFetcher.async(DataFetcher<T>)`` to wrap a ``DataFetcher``. This can be used with static imports to produce more readable code. There is also another implementation that prevents instrumentation from being able to be performed on your schema, if that is a requirement. This behaviour is allowed in the graphql specification and in fact is actively encouraged http://facebook.github.io/graphql/#sec-Query for read only queries. This behaviour makes it unsuitable to be used as a mutation execution strategy. This will ensure that the result follows the specification outlined in http://facebook.github.io/graphql/#sec-Response To avoid the need for re-parse/validate the ``GraphQL.Builder`` allows an instance of ``PreparsedDocumentProvider`` to reuse ``Document`` instances. To ensure you get a JSON result that confirms 100% to the graphql spec, you should call `toSpecification` on the result and then send that back as JSON. To execute a query against a schema build a new ``GraphQL`` object with the appropriate arguments and then call ``execute()``. You can change this behaviour by creating your own ``graphql.execution.DataFetcherExceptionHandler`` exception handling code and giving that to the execution strategy. You can create your own derivation of `GraphqlFieldVisibility` to check what ever you need to do to work out what fields should be visible or not. You can do this by using a `graphql.schema.visibility.GraphqlFieldVisibility` implementation and attaching it to the schema. You can wire in what execution strategy to use when you create the ``GraphQL`` object. You need to send in arguments during that mutation operation, in this case for the variables for ``$ep`` and ``$review`` You would create types like this to handle this mutation : ``graphql-java`` is not opinionated about how you get your domain data objects, that is very much your concern.  It is also not opinionated on user authorisation to that data.  You should push all that logic into your business logic layer code. graphql-java uses fully asynchronous execution techniques when it executes queries.  You can get the ``CompleteableFuture`` to results by calling ``executeAsync()`` like this on how BatchedExecutionStrategy works here.  Its a pretty special case that I don't know how to explain properly with variables: 