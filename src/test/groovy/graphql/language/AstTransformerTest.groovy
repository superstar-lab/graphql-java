package graphql.language

import graphql.TestUtil
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import spock.lang.Specification

import static graphql.language.AstPrinter.printAstCompact
import static graphql.util.TreeTransformerUtil.changeNode
import static graphql.util.TreeTransformerUtil.deleteNode
import static graphql.util.TreeTransformerUtil.insertAfter
import static graphql.util.TreeTransformerUtil.insertBefore

class AstTransformerTest extends Specification {

    def "modify multiple nodes"() {
        def document = TestUtil.parseQuery("{ root { foo { midA { leafA } midB { leafB } } bar { midC { leafC } midD { leafD } } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                if (!node.name.startsWith("mid")) {
                    return TraversalControl.CONTINUE
                }
                String newName = node.name + "-modified"

                Field changedField = node.transform({ builder -> builder.name(newName) })
                return changeNode(context, changedField)
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) ==
                "query {root {foo {midA-modified {leafA} midB-modified {leafB}} bar {midC-modified {leafC} midD-modified {leafD}}}}"
    }

    def "no change at all"() {
        def document = TestUtil.parseQuery("{foo}")

        AstTransformer astTransformer = new AstTransformer()


        when:
        def newDocument = astTransformer.transform(document, new NodeVisitorStub())

        then:
        newDocument == document

    }

    def "one node changed"() {
        def document = TestUtil.parseQuery("{foo}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                Field changedField = node.transform({ builder -> builder.name("foo2") })
                return changeNode(context, changedField)
            }
        }


        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {foo2}"

    }

    def "add new children"() {
        def document = TestUtil.parseQuery("{foo}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {


            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                if (node.name != "foo") return TraversalControl.CONTINUE;
                def newSelectionSet = SelectionSet.newSelectionSet([new Field("a"), new Field("b")]).build()
                Field changedField = node.transform({ builder -> builder.name("foo2").selectionSet(newSelectionSet) })
                return changeNode(context, changedField)
            }
        }


        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {foo2 {a b}}"

    }


    def "reorder children and sub children"() {
        def document = TestUtil.parseQuery("{root { b(b_arg: 1) { y x } a(a_arg:2) { w v } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
                if (node.getChildren().isEmpty()) return TraversalControl.CONTINUE;
                def selections = node.getSelections()
                Collections.sort(selections, { o1, o2 -> (o1.name <=> o2.name) })
                Node changed = node.transform({ builder -> builder.selections(selections) })
                return changeNode(context, changed)
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {root {a(a_arg:2) {v w} b(b_arg:1) {x y}}}"

    }


    def "remove a subtree "() {
        def document = TestUtil.parseQuery("{root { a(arg: 1) { x y } toDelete { x y } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitSelectionSet(SelectionSet node, TraverserContext<Node> context) {
                if (context.getParentContext().thisNode().name == "root") {
                    def newNode = node.transform({ builder -> builder.selections([node.selections[0]]) })
                    return changeNode(context, newNode)
                }
                return TraversalControl.CONTINUE
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {root {a(arg:1) {x y}}}"

    }

    def "delete node"() {
        def document = TestUtil.parseQuery("{root { a(arg: 1) { x y } toDelete { x y } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field field, TraverserContext<Node> context) {
                if (field.name == "toDelete") {
                    return deleteNode(context);
                } else {
                    return TraversalControl.CONTINUE;
                }
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {root {a(arg:1) {x y}}}"

    }

    def "delete multiple nodes and change others"() {
        def document = TestUtil.parseQuery("{root { a(arg: 1) { x1 y1 } b { x2 y2 } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field field, TraverserContext<Node> context) {
                if (field.name == "x1" || field.name == "x2") {
                    return deleteNode(context);
                } else if (field.name == "a") {
                    return changeNode(context, field.transform({ builder -> builder.name("aChanged") }))

                } else if (field.name == "root") {
                    Field addField = new Field("new")
                    def newSelectionSet = field.getSelectionSet().transform({ builder -> builder.selection(addField) })
                    changeNode(context, field.transform({ builder -> builder.selectionSet(newSelectionSet) }))
                } else {
                    return TraversalControl.CONTINUE;
                }
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:

        printAstCompact(newDocument) == "query {root {aChanged(arg:1) {y1} b {y2} new}}"

    }

    def "add sibling after"() {
        def document = TestUtil.parseQuery("{foo}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                return insertAfter(context, new Field("foo2"))
            }
        }


        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {foo foo2}"

    }

    def "add sibling before"() {
        def document = TestUtil.parseQuery("{foo}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                return insertBefore(context, new Field("foo2"))
            }
        }


        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {foo2 foo}"

    }

    def "add sibling before and after"() {
        def document = TestUtil.parseQuery("{foo}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                insertBefore(context, new Field("foo2"))
                insertAfter(context, new Field("foo3"))
                TraversalControl.CONTINUE
            }
        }


        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {foo2 foo foo3}"

    }

    def "delete node and add sibling"() {
        def document = TestUtil.parseQuery("{root { a(arg: 1) { x y } toDelete { x y } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field field, TraverserContext<Node> context) {
                if (field.name == "toDelete") {
                    return deleteNode(context);
                } else if (field.name == "a") {
                    return insertAfter(context, new Field("newOne"))
                } else {
                    return TraversalControl.CONTINUE
                }
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {root {a(arg:1) {x y} newOne}}"

    }

    def "delete node and change sibling"() {
        def document = TestUtil.parseQuery("{root { a(arg: 1) { x y } toDelete { x y } } }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field field, TraverserContext<Node> context) {
                if (field.name == "toDelete") {
                    return deleteNode(context);
                } else if (field.name == "a") {
                    def newNode = field.transform({ builder -> builder.name("a-changed") })
                    return changeNode(context, newNode)
                } else {
                    return TraversalControl.CONTINUE
                }
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {root {a-changed(arg:1) {x y}}}"


    }

    def "change root node"() {
        def document = TestUtil.parseQuery("query A{ field } query B{ fieldB }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitDocument(Document node, TraverserContext<Node> context) {
                def children = node.getChildren()
                children.remove(0)
                def newNode = node.transform({ builder -> builder.definitions(children) })
                changeNode(context, newNode)
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query B {fieldB}"

    }

    def "change different kind of children"() {
        def document = TestUtil.parseQuery("{ field(arg1:1, arg2:2) @directive1 @directive2}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {
                if (node.name == "directive1") {
                    insertAfter(context, new Directive("after1Directive"))
                } else {
                    deleteNode(context)
                    insertAfter(context, new Directive("newDirective2"))
                }
                TraversalControl.CONTINUE
            }

            @Override
            TraversalControl visitArgument(Argument node, TraverserContext<Node> context) {
                if (node.name == "arg1") {
                    deleteNode(context)
                    insertAfter(context, new Argument("newArg1", new IntValue(BigInteger.TEN)))
                } else {
                    insertAfter(context, new Argument("arg3", new IntValue(BigInteger.TEN)))
                }
                TraversalControl.CONTINUE
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {field(newArg1:10,arg2:2,arg3:10) @directive1 @after1Directive @newDirective2}"

    }

    def "insertAfter and then insertBefore"() {
        def document = TestUtil.parseQuery("{ field @directive1 @directive2}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {
                if (node.name == "directive1") {
                    insertAfter(context, new Directive("after"))
                    insertBefore(context, new Directive("before"))
                }
                TraversalControl.CONTINUE
            }

            @Override
            TraversalControl visitArgument(Argument node, TraverserContext<Node> context) {
                TraversalControl.CONTINUE
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {field @before @directive1 @after @directive2}"

    }


    def "mix of all modifications at once"() {
        def document = TestUtil.parseQuery("{ field(arg1:1, arg2:2) @directive1 @directive2}")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {

                if (node.name == "directive1") {
                    insertAfter(context, new Directive("d4"))
                    insertBefore(context, new Directive("d5"))
                } else {
                    insertBefore(context, new Directive("d1"))
                    insertBefore(context, new Directive("d2"))
                    deleteNode(context)
                    insertAfter(context, new Directive("d3"))
                }
                TraversalControl.CONTINUE
            }

            @Override
            TraversalControl visitArgument(Argument node, TraverserContext<Node> context) {
                if (node.name == "arg1") {
                    insertAfter(context, new Argument("a1", new IntValue(BigInteger.TEN)))
                } else {
                    deleteNode(context)
                    insertAfter(context, new Argument("a2", new IntValue(BigInteger.TEN)))
                    insertAfter(context, new Argument("a3", new IntValue(BigInteger.TEN)))
                    insertBefore(context, new Argument("a4", new IntValue(BigInteger.TEN)))
                }
                TraversalControl.CONTINUE
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {field(arg1:1,a1:10,a4:10,a2:10,a3:10) @d5 @directive1 @d4 @d1 @d2 @d3}"

    }

    def "changeNode can be called multiple times"() {
        def document = TestUtil.parseQuery("{ field }")

        AstTransformer astTransformer = new AstTransformer()

        def visitor = new NodeVisitorStub() {

            @Override
            TraversalControl visitField(Field node, TraverserContext<Node> context) {
                changeNode(context, new Field("change1"))
                changeNode(context, new Field("change2"))
                changeNode(context, new Field("change3"))
                TraversalControl.CONTINUE
            }
        }

        when:
        def newDocument = astTransformer.transform(document, visitor)

        then:
        printAstCompact(newDocument) == "query {change3}"

    }


}
