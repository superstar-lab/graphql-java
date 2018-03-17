package graphql.util

import spock.lang.Specification

class TraverserTest extends Specification {
    class Node {
        int number;
        List<Node> children = Collections.emptyList();
    }
    
    def "test pre-order depth-first traversal"() {
        given:
            Node root = new Node(number: 0, children: [
                new Node(number: 1, children: [
                    new Node(number: 3)
                ]),
                new Node(number: 2, children: [
                    new Node(number: 4),
                    new Node(number: 5)
                ])
            ])
        
        when:
            List<Integer> result = new Traverser({Node n -> n.children})
                .traverse(root, new ArrayList<Integer>(), new TraverserVisitor<Node, List<Node>>() {
                    public Object enter (TraverserContext<? super Node> context, List<Node> data) {
                        data.add(context.thisNode().number);
                        return data;
                    }
                    
                    public Object leave (TraverserContext<? super Node> context, List<Node> data) {
                        return data;
                    }
                })
            
        then:
            assert result == [0, 1, 3, 2, 4, 5]
    }
    
    def "test post-order depth-first traversal"() {
        given:
            Node root = new Node(number: 0, children: [
                new Node(number: 1, children: [
                    new Node(number: 3)
                ]),
                new Node(number: 2, children: [
                    new Node(number: 4),
                    new Node(number: 5)
                ])
            ])
        
        when:
            List<Integer> result = new Traverser({Node n -> n.children})
                .traverse(root, new ArrayList<Integer>(), new TraverserVisitor<Node, List<Node>>() {
                    public Object enter (TraverserContext<? super Node> context, List<Node> data) {
                        return data;
                    }
                    
                    public Object leave (TraverserContext<? super Node> context, List<Node> data) {
                        data.add(context.thisNode().number);
                        return data;
                    }
                })
            
        then:
            assert result == [3, 1, 4, 5, 2, 0]
    }
    
    def "test breadth-first traversal"() {
        given:
            Node root = new Node(number: 0, children: [
                new Node(number: 1, children: [
                    new Node(number: 3)
                ]),
                new Node(number: 2, children: [
                    new Node(number: 4),
                    new Node(number: 5)
                ])
            ])
        
        when:
            List<Integer> result = new Traverser(new TraverserQueue<Node>(), {Node n -> n.children})
                .traverse(root, new ArrayList<Integer>(), new TraverserVisitor<Node, List<Node>>() {
                    public Object enter (TraverserContext<? super Node> context, List<Node> data) {
                        data.add(context.thisNode().number);
                        return data;
                    }
                    
                    public Object leave (TraverserContext<? super Node> context, List<Node> data) {
                        return data;
                    }
                })
            
        then:
            assert result == [0, 1, 2, 3, 4, 5]
    }
}

