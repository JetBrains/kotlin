import Inheritance
import Testing

// An exported class is rooted in NSObject, and kotlin.Any's reverse adapters send
// -description/-hash/-isEqual:. So those selectors -- not the Kotlin-named members -- are the
// native extension points, and they must agree with Kotlin dispatch in both directions.

@Test
func kotlinOverridesOfAnyMembersAreVisibleNatively() throws {
    let value = Describable()
    let other = Describable()

    #expect(String(describing: value) == "kotlin-describable")
    #expect(value.hash == 11)
    #expect(value.isEqual(other))

    #expect(callToString(value: value) == "kotlin-describable")
    #expect(callHashCode(value: value) == 11)
    #expect(callEquals(lhs: value, rhs: other))
}

@Test
func swiftSubclassInheritsKotlinOverridesOfAnyMembers() throws {
    // No overrides at all: the inherited members must still reach Kotlin without recursing through
    // the reverse bridge installed in the subclass's own vtable slots.
    class InheritingDescribable: Describable {}

    let value = InheritingDescribable()

    #expect(String(describing: value) == "kotlin-describable")
    #expect(value.hash == 11)
    #expect(value.isEqual(Describable()))

    #expect(callToString(value: value) == "kotlin-describable")
    #expect(callHashCode(value: value) == 11)
    #expect(callEquals(lhs: value, rhs: Describable()))
}

@Test
func swiftOverridesOfNativeAnyMembersWinOverKotlin() throws {
    class SwiftDescribable: Describable {
        override var description: Swift.String { "swift-describable" }
        override var hash: Swift.Int { 22 }
        override func isEqual(_ other: Any?) -> Swift.Bool { other is SwiftDescribable }
    }

    let value = SwiftDescribable()

    // The native spellings see the Swift override, trivially.
    #expect(String(describing: value) == "swift-describable")
    #expect(value.hash == 22)
    #expect(!value.isEqual(Describable()))

    // Kotlin dispatch observes the same overrides: the Swift subclass is the most derived
    // implementation of these slots, and `kotlin.Any`'s reverse adapters send the selectors.
    #expect(callToString(value: value) == "swift-describable")
    #expect(callHashCode(value: value) == 22)
    #expect(!callEquals(lhs: value, rhs: Describable()))
}

@Test
func swiftOverrideOfNativeAnyMemberCanCallSuper() throws {
    class SwiftDescribable: Describable {
        override var description: Swift.String { "swift+" + super.description }
        override var hash: Swift.Int { super.hash + 1 }
        override func isEqual(_ other: Any?) -> Swift.Bool { super.isEqual(other) }
    }

    let value = SwiftDescribable()

    // `super` has to land in the Kotlin implementation the subclass shadows.
    #expect(String(describing: value) == "swift+kotlin-describable")
    #expect(value.hash == 12)
    #expect(value.isEqual(Describable()))

    #expect(callToString(value: value) == "swift+kotlin-describable")
    #expect(callHashCode(value: value) == 12)
    #expect(callEquals(lhs: value, rhs: Describable()))
}

// Two synthesized levels between the receiver and the nearest compiled Kotlin class: the walk to
// that class has to cope with more than one Swift level, and `super` has to arrive there too.
@Test
func twoLevelSwiftHierarchyOverKotlinOverrides() throws {
    class FirstLevel: Describable {}
    class SecondLevel: FirstLevel {
        override var description: Swift.String { "swift+" + super.description }
        override var hash: Swift.Int { super.hash + 1 }
    }

    let inherited = FirstLevel()
    #expect(String(describing: inherited) == "kotlin-describable")
    #expect(inherited.hash == 11)
    #expect(callToString(value: inherited) == "kotlin-describable")
    #expect(callHashCode(value: inherited) == 11)

    let decorated = SecondLevel()
    #expect(String(describing: decorated) == "swift+kotlin-describable")
    #expect(decorated.hash == 12)
    #expect(callToString(value: decorated) == "swift+kotlin-describable")
    #expect(callHashCode(value: decorated) == 12)
}

// `DescribableDerived` overrides what `DescribableBase` already overrode. The nearest compiled
// implementation has to win -- reaching for the least derived one would render "kotlin-base".
@Test
func nearestKotlinOverrideOfAnyMemberWins() throws {
    class SwiftLeaf: DescribableDerived {}

    let value = SwiftLeaf()

    #expect(String(describing: value) == "kotlin-derived")
    #expect(value.hash == 2)
    #expect(callToString(value: value) == "kotlin-derived")
    #expect(callHashCode(value: value) == 2)
}

// Mixed ownership of the three slots: `toString` resolves to the Kotlin class, `hash` to the Swift
// override, `equals` to kotlin.Any.
@Test
func partiallyOverriddenKotlinClassMixesWithSwiftOverride() throws {
    class SwiftHash: PartiallyDescribable {
        override var hash: Swift.Int { 33 }
    }

    let value = SwiftHash()

    #expect(String(describing: value) == "kotlin-partial")
    #expect(callToString(value: value) == "kotlin-partial")
    #expect(value.hash == 33)
    #expect(callHashCode(value: value) == 33)

    // kotlin.Any's reference equality, reached through -isEqual:.
    #expect(callEquals(lhs: value, rhs: value))
    #expect(!callEquals(lhs: value, rhs: SwiftHash()))
}

// `==` is not exported from Kotlin: it resolves to NSObject's Equatable conformance, which forwards
// to -isEqual: -- the same slot Kotlin's `==` reaches.
@Test
func equalityOperatorResolvesToTheNativeSpelling() throws {
    class Distinct: Describable {
        override func isEqual(_ other: Any?) -> Swift.Bool { other is Distinct }
    }

    let value = Distinct()
    let other = Distinct()
    let kotlinValue = Describable()

    #expect(value == other)
    #expect(value != kotlinValue)
    #expect(callEquals(lhs: value, rhs: other))
    #expect(!callEquals(lhs: value, rhs: kotlinValue))

    // Kotlin's own `equals` considers every Describable equal, and `==` sees that too.
    #expect(kotlinValue == Describable())
}

// The identity the two languages present has to be the same one: the same object has to land in the
// same bucket and compare the same way in a Swift Set and in a Kotlin HashSet.
@Test
func swiftAndKotlinHashedCollectionsAgree() throws {
    class Distinct: Describable {
        override var hash: Swift.Int { 99 }
        override func isEqual(_ other: Any?) -> Swift.Bool { other is Distinct }
    }

    let kotlinValue = Describable()
    let equalToKotlinValue = Describable()
    let swiftValue = Distinct()
    let equalToSwiftValue = Distinct()

    let kotlinSet: Set<Describable> = [kotlinValue]
    #expect(kotlinSet.contains(equalToKotlinValue))
    #expect(hashSetContains(element: kotlinValue, probe: equalToKotlinValue))

    let swiftSet: Set<Describable> = [swiftValue]
    #expect(swiftSet.contains(equalToSwiftValue))
    #expect(hashSetContains(element: swiftValue, probe: equalToSwiftValue))

    // Different hash and a rejecting -isEqual:, so neither runtime finds the other's value.
    #expect(!swiftSet.contains(kotlinValue))
    #expect(!hashSetContains(element: swiftValue, probe: kotlinValue))
}
