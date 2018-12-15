package graphql.language;

import graphql.PublicApi;
import graphql.util.DefaultTraverserContext;
import graphql.util.TraversalControl;
import graphql.util.Traverser;
import graphql.util.TraverserContext;
import graphql.util.TraverserVisitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Lets you traverse a {@link Node} tree.
 */
@PublicApi
public class NodeTraverser {


    /**
     * Used by depthFirst to indicate via {@link TraverserContext#getVar(Class)} if the visit happens inside the ENTER or LEAVE phase.
     */
    public enum LeaveOrEnter {
        LEAVE,
        ENTER
    }

    private final Map<Class<?>, Object> rootVars;
    private final Function<? super Node, ? extends List<Node>> getChildren;

    public NodeTraverser(Map<Class<?>, Object> rootVars, Function<? super Node, ? extends List<Node>> getChildren) {
        this.rootVars = rootVars;
        this.getChildren = getChildren;
    }

    public NodeTraverser() {
        this(Collections.emptyMap(), Node::getChildren);
    }


    /**
     * depthFirst traversal with a enter/leave phase.
     *
     * @param nodeVisitor the visitor of the nodes
     * @param root        the root node
     */
    public void depthFirst(NodeVisitor nodeVisitor, Node root) {
        depthFirst(nodeVisitor, Collections.singleton(root));
    }

    /**
     * depthFirst traversal with a enter/leave phase.
     *
     * @param nodeVisitor the visitor of the nodes
     * @param roots       the root nodes
     */
    public void depthFirst(NodeVisitor nodeVisitor, Collection<? extends Node> roots) {
        TraverserVisitor<Node> nodeTraverserVisitor = new TraverserVisitor<Node>() {

            @Override
            public TraversalControl enter(TraverserContext<Node> context) {
                context.setVar(LeaveOrEnter.class, LeaveOrEnter.ENTER);
                return context.thisNode().accept(context, nodeVisitor);
            }

            @Override
            public TraversalControl leave(TraverserContext<Node> context) {
                context.setVar(LeaveOrEnter.class, LeaveOrEnter.LEAVE);
                return context.thisNode().accept(context, nodeVisitor);
            }

        };
        doTraverse(roots, nodeTraverserVisitor);
    }

    /**
     * Version of {@link #preOrder(NodeVisitor, Collection)} with one root.
     *
     * @param nodeVisitor the visitor of the nodes
     * @param root        the root node
     */
    public void preOrder(NodeVisitor nodeVisitor, Node root) {
        preOrder(nodeVisitor, Collections.singleton(root));
    }

    /**
     * Pre-Order traversal: This is a specialized version of depthFirst with only the enter phase.
     *
     * @param nodeVisitor the visitor of the nodes
     * @param roots       the root nodes
     */
    public void preOrder(NodeVisitor nodeVisitor, Collection<? extends Node> roots) {
        TraverserVisitor<Node> nodeTraverserVisitor = new TraverserVisitor<Node>() {

            @Override
            public TraversalControl enter(TraverserContext<Node> context) {
                context.setVar(LeaveOrEnter.class, LeaveOrEnter.ENTER);
                return context.thisNode().accept(context, nodeVisitor);
            }

            @Override
            public TraversalControl leave(TraverserContext<Node> context) {
                return TraversalControl.CONTINUE;
            }

        };
        doTraverse(roots, nodeTraverserVisitor);

    }

    /**
     * Version of {@link #postOrder(NodeVisitor, Collection)} with one root.
     *
     * @param nodeVisitor the visitor of the nodes
     * @param root        the root node
     */
    public void postOrder(NodeVisitor nodeVisitor, Node root) {
        postOrder(nodeVisitor, Collections.singleton(root));
    }

    /**
     * Post-Order traversal: This is a specialized version of depthFirst with only the leave phase.
     *
     * @param nodeVisitor the visitor of the nodes
     * @param roots       the root nodes
     */
    public void postOrder(NodeVisitor nodeVisitor, Collection<? extends Node> roots) {
        TraverserVisitor<Node> nodeTraverserVisitor = new TraverserVisitor<Node>() {

            @Override
            public TraversalControl enter(TraverserContext<Node> context) {
                return TraversalControl.CONTINUE;
            }

            @Override
            public TraversalControl leave(TraverserContext<Node> context) {
                context.setVar(LeaveOrEnter.class, LeaveOrEnter.LEAVE);
                return context.thisNode().accept(context, nodeVisitor);
            }

        };
        doTraverse(roots, nodeTraverserVisitor);
    }

    private void doTraverse(Collection<? extends Node> roots, TraverserVisitor traverserVisitor) {
        Traverser<Node> nodeTraverser = Traverser.depthFirst(this.getChildren);
        nodeTraverser.rootVars(rootVars);
        nodeTraverser.traverse(roots, traverserVisitor);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    public static <T> T oneVisitWithResult(Node node, NodeVisitor nodeVisitor) {
        DefaultTraverserContext<Node> context = DefaultTraverserContext.simple(node);
        node.accept(context, nodeVisitor);
        return (T) context.getNewAccumulate();
    }

}
