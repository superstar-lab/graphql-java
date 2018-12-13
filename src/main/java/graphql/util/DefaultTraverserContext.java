package graphql.util;

import graphql.Internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertTrue;

@Internal
public class DefaultTraverserContext<T> implements TraverserContext<T> {

    private final T curNode;
    private T newNode;
    private final TraverserContext<T> parent;
    private final Set<T> visited;
    private final Map<Class<?>, Object> vars;
    private final Object sharedContextData;

    private Object newAccValue;
    private boolean hasNewAccValue;
    private Object curAccValue;
    private final NodePosition position;
    private final boolean isRootContext;

    public DefaultTraverserContext(T curNode,
                                   TraverserContext<T> parent,
                                   Set<T> visited,
                                   Map<Class<?>, Object> vars,
                                   Object sharedContextData,
                                   NodePosition position,
                                   boolean isRootContext) {
        this.curNode = curNode;
        this.parent = parent;
        this.visited = visited;
        this.vars = vars;
        this.sharedContextData = sharedContextData;
        this.position = position;
        this.isRootContext = isRootContext;
    }

    public static <T> DefaultTraverserContext<T> dummy() {
        return new DefaultTraverserContext<>(null, null, null, null, null, null, true);
    }

    public static <T> DefaultTraverserContext<T> simple(T node) {
        return new DefaultTraverserContext<>(node, null, null, null, null, null, true);
    }

    @Override
    public T thisNode() {
        if (newNode != null) {
            return newNode;
        }
        return curNode;
    }


    @Override
    public void changeNode(T newNode) {
        assertNotNull(newNode);
        assertTrue(this.newNode == null, "node can only be changed once");
        this.newNode = newNode;
    }


    @Override
    public TraverserContext<T> getParentContext() {
        return parent;
    }

    @Override
    public List<T> getParentNodes() {
        List<T> result = new ArrayList<>();
        TraverserContext<T> curContext = parent;
        while (!curContext.isRootContext()) {
            result.add(curContext.thisNode());
            curContext = curContext.getParentContext();
        }
        return result;
    }

    @Override
    public Set<T> visitedNodes() {
        return visited;
    }

    @Override
    public boolean isVisited() {
        return visited.contains(curNode);
    }

    @Override
    public <S> S getVar(Class<? super S> key) {
        return (S) key.cast(vars.get(key));
    }

    @Override
    public <S> TraverserContext<T> setVar(Class<? super S> key, S value) {
        vars.put(key, value);
        return this;
    }

    @Override
    public void setAccumulate(Object accumulate) {
        hasNewAccValue = true;
        newAccValue = accumulate;
    }

    @Override
    public <U> U getNewAccumulate() {
        if (hasNewAccValue) {
            return (U) newAccValue;
        } else {
            return (U) curAccValue;
        }
    }

    @Override
    public <U> U getCurrentAccumulate() {
        return (U) curAccValue;
    }


    @Override
    public Object getSharedContextData() {
        return sharedContextData;
    }

    /*
     * PRIVATE: Used by {@link Traverser}
     */
    void setCurAccValue(Object curAccValue) {
        hasNewAccValue = false;
        this.curAccValue = curAccValue;
    }

    @Override
    public NodePosition getPosition() {
        return position;
    }

    @Override
    public boolean isRootContext() {
        return isRootContext;
    }

}
