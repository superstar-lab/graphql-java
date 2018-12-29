package graphql.execution.nextgen;

import graphql.Internal;
import graphql.execution.Async;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStepInfoFactory;
import graphql.execution.MergedField;
import graphql.execution.nextgen.result.ExecutionResultMultiZipper;
import graphql.execution.nextgen.result.ExecutionResultNode;
import graphql.execution.nextgen.result.ExecutionResultZipper;
import graphql.execution.nextgen.result.NamedResultNode;
import graphql.execution.nextgen.result.ObjectExecutionResultNode;
import graphql.execution.nextgen.result.ResultNodesUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Internal
public class DefaultExecutionStrategy implements ExecutionStrategy {

    ExecutionStepInfoFactory executionInfoFactory;
    ValueFetcher valueFetcher;
    ResultNodesCreator resultNodesCreator = new ResultNodesCreator();

    private final ExecutionContext executionContext;
    private FetchedValueAnalyzer fetchedValueAnalyzer;

    public DefaultExecutionStrategy(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.fetchedValueAnalyzer = new FetchedValueAnalyzer(executionContext);
        this.valueFetcher = new ValueFetcher(executionContext);
        this.executionInfoFactory = new ExecutionStepInfoFactory();
    }

    /*
     * the fundamental algorithm is:
     * - fetch sub selection and analyze it
     * - convert the fetched value analysis into result node
     * - get all unresolved result nodes and resolve the sub selection (start again recursively)
     */
    @Override
    public CompletableFuture<ObjectExecutionResultNode.RootExecutionResultNode> execute(FieldSubSelection fieldSubSelection) {
        return resolveSubSelection(fieldSubSelection)
                .thenApply(ObjectExecutionResultNode.RootExecutionResultNode::new);
    }

    // recursive entry point
    private CompletableFuture<List<NamedResultNode>> resolveSubSelection(FieldSubSelection fieldSubSelection) {
        List<CompletableFuture<NamedResultNode>> result = fetchSubSelection(fieldSubSelection)
                .stream().map(namedResultNodeCF -> namedResultNodeCF.thenCompose(this::resolveNode)).collect(toList());
        return Async.each(result);
    }

    // ----------- fetching subSelection into ResultNode
    private List<CompletableFuture<NamedResultNode>> fetchSubSelection(FieldSubSelection fieldSubSelection) {
        List<CompletableFuture<FetchedValueAnalysis>> fetchedValueAnalysisList = fetchAndAnalyze(fieldSubSelection);
        return Async.map(fetchedValueAnalysisList, fetchedValueAnalysis -> {
            ExecutionResultNode resultNode = resultNodesCreator.createResultNode(fetchedValueAnalysis);
            return new NamedResultNode(fetchedValueAnalysis.getName(), resultNode);
        });
    }

    private List<CompletableFuture<FetchedValueAnalysis>> fetchAndAnalyze(FieldSubSelection fieldSubSelection) {
        List<CompletableFuture<FetchedValueAnalysis>> fetchedValues = fieldSubSelection.getSubFields().entrySet().stream()
                .map(entry -> mapMergedField(fieldSubSelection.getSource(), entry.getKey(), entry.getValue(), fieldSubSelection.getExecutionStepInfo()))
                .collect(toList());
        return fetchedValues;
    }

    private CompletableFuture<FetchedValueAnalysis> mapMergedField(Object source, String key, MergedField mergedField, ExecutionStepInfo executionStepInfo) {
        ExecutionStepInfo newExecutionStepInfo = executionInfoFactory.newExecutionStepInfoForSubField(executionContext, mergedField, executionStepInfo);
        return valueFetcher
                .fetchValue(source, mergedField, newExecutionStepInfo)
                .thenApply(fetchValue -> analyseValue(fetchValue, key, mergedField, newExecutionStepInfo));
    }

    private FetchedValueAnalysis analyseValue(FetchedValue fetchedValue, String name, MergedField field, ExecutionStepInfo executionInfo) {
        FetchedValueAnalysis fetchedValueAnalysis = fetchedValueAnalyzer.analyzeFetchedValue(fetchedValue, name, field, executionInfo);
        return fetchedValueAnalysis;
    }

    // ----------- get all unresolved Nodes and recursively resolves them
    // this method is actually an async transformer of specific child nodes
    private CompletableFuture<NamedResultNode> resolveNode(NamedResultNode namedResultNode) {
        // can be empty
        ExecutionResultMultiZipper unresolvedMultiZipper = ResultNodesUtil.getUnresolvedNodes(namedResultNode.getNode());
        // must be a unresolved Node
        List<CompletableFuture<ExecutionResultZipper>> cfList = unresolvedMultiZipper
                .getZippers()
                .stream()
                .map(this::resolveUnresolvedNode)
                .collect(Collectors.toList());
        return Async
                .each(cfList)
                .thenApply(unresolvedMultiZipper::withZippers)
                .thenApply(ExecutionResultMultiZipper::toRootNode)
                .thenApply(namedResultNode::withNode);
    }

    // recursive call back to resolveSubSelection
    private CompletableFuture<ExecutionResultZipper> resolveUnresolvedNode(ExecutionResultZipper unresolvedNodeZipper) {
        FetchedValueAnalysis fetchedValueAnalysis = unresolvedNodeZipper.getCurNode().getFetchedValueAnalysis();
        return resolveSubSelection(fetchedValueAnalysis.getFieldSubSelection())
                .thenApply(resolvedChildMap -> unresolvedNodeZipper.withNode(new ObjectExecutionResultNode(fetchedValueAnalysis, resolvedChildMap)));
    }


}
