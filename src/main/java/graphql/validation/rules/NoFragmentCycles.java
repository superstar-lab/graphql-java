package graphql.validation.rules;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import graphql.Internal;
import graphql.language.Definition;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Node;
import graphql.validation.AbstractRule;
import graphql.validation.DocumentVisitor;
import graphql.validation.LanguageTraversal;
import graphql.validation.ValidationContext;
import graphql.validation.ValidationErrorCollector;
import graphql.validation.ValidationErrorType;

@Internal
public class NoFragmentCycles extends AbstractRule {

    private final Map<String, List<FragmentSpread>> fragmentSpreads = new LinkedHashMap<>();
    private final HashSet<String> checked = new HashSet<>();


    public NoFragmentCycles(ValidationContext validationContext, ValidationErrorCollector validationErrorCollector) {
        super(validationContext, validationErrorCollector);
        prepareFragmentMap();
    }

    private void prepareFragmentMap() {
        List<Definition> definitions = getValidationContext().getDocument().getDefinitions();
        for (Definition definition : definitions) {
            if (definition instanceof FragmentDefinition) {
                FragmentDefinition fragmentDefinition = (FragmentDefinition) definition;
                fragmentSpreads.put(fragmentDefinition.getName(), gatherSpreads(fragmentDefinition));
            }
        }
    }


    private List<FragmentSpread> gatherSpreads(FragmentDefinition fragmentDefinition) {
        final List<FragmentSpread> fragmentSpreads = new ArrayList<>();
        DocumentVisitor visitor = new DocumentVisitor() {
            @Override
            public void enter(Node node, List<Node> path) {
                if (node instanceof FragmentSpread) {
                    fragmentSpreads.add((FragmentSpread) node);
                }
            }

            @Override
            public void leave(Node node, List<Node> path) {

            }
        };

        new LanguageTraversal().traverse(fragmentDefinition, visitor);
        return fragmentSpreads;
    }


    @Override
    public void checkFragmentDefinition(FragmentDefinition fragmentDefinition) {
        List<FragmentSpread> spreadPath = new ArrayList<>();
        detectCycleRecursive(fragmentDefinition.getName(), fragmentDefinition.getName(), spreadPath);
    }

    private void detectCycleRecursive(String fragmentName, String initialName, List<FragmentSpread> spreadPath) {
        List<FragmentSpread> fragmentSpreads = this.fragmentSpreads.get(fragmentName);
        if (checked.contains(fragmentName)) {
            return;
        }

        if (fragmentSpreads == null) {
            // KnownFragmentNames will have picked this up.  Lets not NPE
            return;
        }

        // JMB TODO: TIDY
        /**
         * JMB NOTES:
         *
         * The complexity here is something close to N*N*D, where N is the number of fragments and
         * D is an average path depth.
         *
         * This feels possible to linearize or do with dynamic programming
         * It also *certainly* repeats work
         */



        outer:
        for (FragmentSpread fragmentSpread : fragmentSpreads) {

            if (fragmentSpread.getName().equals(initialName)) {
                String message = "Fragment cycles not allowed";
                addError(ValidationErrorType.FragmentCycle, spreadPath, message);
                continue;
            }
            for (FragmentSpread spread : spreadPath) {
                if (spread.equals(fragmentSpread)) {
                    continue outer;
                }
            }
            spreadPath.add(fragmentSpread);
            detectCycleRecursive(fragmentSpread.getName(), initialName, spreadPath);
            spreadPath.remove(spreadPath.size() - 1);
        }
        checked.add(fragmentName);
    }
}
