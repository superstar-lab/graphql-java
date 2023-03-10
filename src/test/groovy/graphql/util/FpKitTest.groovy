package graphql.util


import spock.lang.Specification

import java.util.function.Supplier

class FpKitTest extends Specification {

    class IterableThing implements Iterable {
        Iterable delegate

        IterableThing(Iterable delegate) {
            this.delegate = delegate
        }

        @Override
        Iterator iterator() {
            return delegate.iterator()
        }
    }

    def "toCollection works as expected"() {
        def expected = ["a", "b", "c"]
        def actual

        when:
        def array = ["a", "b", "c"].toArray()
        actual = FpKit.toCollection(array)
        then:
        actual == expected

        when:
        Set set = ["a", "b", "c"].toSet()
        actual = FpKit.toCollection(set)
        then:
        actual == expected.toSet()

        when:
        List list = ["a", "b", "c"].toList()
        actual = FpKit.toCollection(list)
        then:
        actual == expected

        when:
        IterableThing iterableThing = new IterableThing(["a", "b", "c"])
        actual = FpKit.toCollection(iterableThing)
        then:
        actual == expected
    }

    void "memoized supplier"() {

        def count = 0
        Supplier<Integer> supplier = { -> count++; return count }

        when:
        def memoizeSupplier = FpKit.intraThreadMemoize(supplier)
        def val1 = supplier.get()
        def val2 = supplier.get()
        def memoVal1 = memoizeSupplier.get()
        def memoVal2 = memoizeSupplier.get()

        then:
        val1 == 1
        val2 == 2

        memoVal1 == 3
        memoVal2 == 3
    }

    def "toListOrSingletonList works"() {
        def birdArr = ["Parrot", "Cockatiel", "Pigeon"] as String[]

        when:
        def l = FpKit.toListOrSingletonList(birdArr)
        then:
        l == ["Parrot", "Cockatiel", "Pigeon"]

        when:
        l = FpKit.toListOrSingletonList(["Parrot", "Cockatiel", "Pigeon"])
        then:
        l == ["Parrot", "Cockatiel", "Pigeon"]

        when:
        l = FpKit.toListOrSingletonList(["Parrot", "Cockatiel", "Pigeon"].stream())
        then:
        l == ["Parrot", "Cockatiel", "Pigeon"]

        when:
        l = FpKit.toListOrSingletonList(["Parrot", "Cockatiel", "Pigeon"].stream().iterator())
        then:
        l == ["Parrot", "Cockatiel", "Pigeon"]

        when:
        l = FpKit.toListOrSingletonList("Parrot")
        then:
        l == ["Parrot"]
    }

    def "set intersection works"() {
        def set1 = ["A","B","C"] as Set
        def set2 = ["A","C","D"] as Set
        def singleSetA = ["A"] as Set
        def disjointSet = ["X","Y"] as Set

        when:
        def intersection = FpKit.intersection(set1, set2)
        then:
        intersection == ["A","C"] as Set

        when: // reversed parameters
        intersection = FpKit.intersection(set2, set1)
        then:
        intersection == ["A","C"] as Set

        when: // singles
        intersection = FpKit.intersection(set1, singleSetA)
        then:
        intersection == ["A"] as Set

        when: // singles reversed
        intersection = FpKit.intersection(singleSetA, set1)
        then:
        intersection == ["A"] as Set

        when: // disjoint
        intersection = FpKit.intersection(set1, disjointSet)
        then:
        intersection.isEmpty()

        when: // disjoint reversed
        intersection = FpKit.intersection(disjointSet,set1)
        then:
        intersection.isEmpty()
    }
}
