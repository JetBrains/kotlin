import Inheritance
import Testing

@Test
func swiftSubclassWorksThroughAbstractKotlinAncestry() throws {
    class SwiftAbstractLeaf: ConcreteAbstractBranch {
        override func abstractValue() -> String { "swift-abstract" }
    }

    let value = SwiftAbstractLeaf()
    #expect(callAbstractValue(value: value) == "swift-abstract")
    #expect(callAbstractConcrete(value: value) == "abstract-concrete")
}

@Test
func swiftSubclassUsesPrimarySecondaryAndDefaultedConstructorAncestry() throws {
    class SwiftConstructorLeaf: ConstructorBase {
        override func constructorValue() -> String { "swift>" + super.constructorValue() }
    }
    class SwiftDefaultConstructorLeaf: DefaultConstructorBranch {
        override func constructorValue() -> String { "swift>" + super.constructorValue() }
    }

    let primary = SwiftConstructorLeaf(constructorOrigin: "primary-explicit")
    #expect(callConstructorValue(value: primary) == "swift>primary-explicit")

    let secondary = SwiftConstructorLeaf(number: 7)
    #expect(callConstructorValue(value: secondary) == "swift>secondary:7")

    let defaulted = SwiftDefaultConstructorLeaf()
    #expect(callConstructorValue(value: defaulted) == "swift>primary-default")
}

@Test
func directSwiftSubclassOfAbstractKotlinClass() throws {
    // KT-87947: a Swift class directly inherits an abstract Kotlin class — no concrete Kotlin class in
    // between — and overrides its abstract member.
    class DirectSwiftAbstractLeaf: AbstractRoot {
        override func abstractValue() -> String { "swift-direct-abstract" }
    }

    #expect(callAbstractValue(value: DirectSwiftAbstractLeaf()) == "swift-direct-abstract")
    // A Kotlin type check against an interface reads the interface table of the Swift subclass.
    #expect(!isLabeled(value: DirectSwiftAbstractLeaf()))
}

@Test
func directSwiftSubclassOfAbstractKotlinClassImplementingInterface() throws {
    // The abstract Kotlin class implements the interface: Kotlin-side interface dispatch has to reach
    // the inherited Kotlin implementation, which in turn reaches the Swift override of the abstract member.
    class SwiftLabeled: AbstractLabeled {
        override func kind() -> String { "swift" }
    }
    let value = SwiftLabeled()

    #expect(value.label() == "labeled:swift")
    #expect(callLabel(value: value) == "labeled:swift")
}

@Test
func directSwiftSubclassOfAbstractKotlinClassAdoptsInterface() throws {
    // The abstract Kotlin class does not implement the interface, the Swift subclass adopts it itself:
    // Kotlin-side type checks and interface dispatch have to see the Swift implementation.
    class SwiftAdoptingLeaf: AbstractRoot, Labeled {
        override func abstractValue() -> String { "swift-adopting" }
        func label() -> String { "swift-label" }
    }
    let value = SwiftAdoptingLeaf()

    #expect(isLabeled(value: value))
    #expect(callLabel(value: value) == "swift-label")
    #expect(callAbstractValue(value: value) == "swift-adopting")
}

@Test
func swiftCanInheritAbstractKotlinClass() throws {
    // A Swift class inherits an abstract Kotlin class and overrides its abstract member. `super.init`
    // (inherited implicitly) runs the abstract class's constructor, initializing `prefix`. The
    // inherited concrete `decorated()` reaches the ctor-initialized state AND the Swift override of the
    // abstract `greeting()` via a virtual self-call — proving the abstract ctor actually ran (otherwise
    // `prefix` would be empty).
    class SwiftGreeter: AbstractGreeter {
        override func greeting() -> String { "swift-greeting" }
    }
    let g = SwiftGreeter()

    // Abstract member overridden in Swift (direct dispatch).
    #expect(g.greeting() == "swift-greeting")
    // Inherited concrete method: ctor-initialized `prefix` + Swift `greeting()`.
    #expect(g.decorated() == "kotlin-prefix:swift-greeting")

    // Kotlin-side dispatch reaches the Swift override and the inherited concrete method.
    #expect(callGreeting(g: g) == "swift-greeting")
    #expect(callDecorated(g: g) == "kotlin-prefix:swift-greeting")
}

@Test
func swiftCanInheritAbstractKotlinClassWithConstructorArgs() throws {
    // The abstract Kotlin class has a constructor parameter. The Swift subclass passes it through
    // `super.init`, and the inherited concrete `total()` combines the ctor-initialized `start` with the
    // Swift override of the abstract `step()`.
    class SwiftCounter: AbstractCounter {
        private let n: Int32
        init(n: Int32) {
            self.n = n
            super.init(start: 100)
        }
        override func step() -> Int32 { n }
    }
    let c = SwiftCounter(n: 5)

    #expect(c.step() == 5)
    // start (100, from the abstract class's constructor) + step (5, from the Swift override).
    #expect(c.total() == 105)
    #expect(callTotal(c: c) == 105)
}
