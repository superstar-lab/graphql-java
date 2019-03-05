package graphql.analysis;

import graphql.PublicApi;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.util.TraverserContext;

@PublicApi
public interface QueryVisitorFragmentEnvironment {
    FragmentDefinition getFragmentDefinition();

    TraverserContext<Node> getTraverserContext();
}
