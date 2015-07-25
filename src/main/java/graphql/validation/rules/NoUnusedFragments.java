package graphql.validation.rules;


import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.OperationDefinition;
import graphql.validation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NoUnusedFragments extends AbstractRule {


    private final List<FragmentDefinition> allDeclaredFragments = new ArrayList<>();

    private List<String> usedFragments = new ArrayList<>();
    private Map<String, List<String>> spreadsInDefinition = new LinkedHashMap<>();
    private final List<List<String>>  fragmentsUsedDirectlyInOperation = new ArrayList<>();

    public NoUnusedFragments(ValidationContext validationContext, ValidationErrorCollector validationErrorCollector) {
        super(validationContext, validationErrorCollector);
    }

    @Override
    public void checkOperationDefinition(OperationDefinition operationDefinition) {
        usedFragments = new ArrayList<>();
        fragmentsUsedDirectlyInOperation.add(usedFragments);
    }


    @Override
    public void checkFragmentSpread(FragmentSpread fragmentSpread) {
        usedFragments.add(fragmentSpread.getName());
    }

    @Override
    public void checkFragmentDefinition(FragmentDefinition fragmentDefinition) {
        allDeclaredFragments.add(fragmentDefinition);
        usedFragments = new ArrayList<>();
        spreadsInDefinition.put(fragmentDefinition.getName(), usedFragments);
    }

    @Override
    public void documentFinished(Document document) {

        List<String> allUsedFragments = new ArrayList<>();
        for(List<String> fragmentsInOneOperation : fragmentsUsedDirectlyInOperation){
            for(String fragment : fragmentsInOneOperation){
                collectUsedFragmentsInDefinition(allUsedFragments,fragment);
            }
        }

        for(FragmentDefinition fragmentDefinition : allDeclaredFragments){
            if(!allUsedFragments.contains(fragmentDefinition.getName())){
                addError(new ValidationError(ValidationErrorType.UnusedFragment));
            }
        }

    }

    private void collectUsedFragmentsInDefinition(List<String> result,String fragmentName){
        if(result.contains(fragmentName)) return;
        result.add(fragmentName);
        List<String> spreadList = spreadsInDefinition.get(fragmentName);
        for(String fragment : spreadList){
            collectUsedFragmentsInDefinition(result,fragment);
        }

    }

}