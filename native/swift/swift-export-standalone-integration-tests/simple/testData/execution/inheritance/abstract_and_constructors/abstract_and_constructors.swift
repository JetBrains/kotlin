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

//  `.disabled(...)` cannot be used here, because a disabled test is still compiled and this body does not
//  compile. Delete the `#if`/`#endif` (and add `import KotlinRuntime`) once KT-87947 is
// fixed;
#if KT87947_FIXED
@Test(.disabled("KT-87947:can't inherit Kotlin abstract class"))
func directSwiftSubclassOfAbstractKotlinClass() throws {
    class DirectSwiftAbstractLeaf: AbstractRoot {
        override func abstractValue() -> String { "swift-direct-abstract" }
    }

    #expect(callAbstractValue(value: DirectSwiftAbstractLeaf()) == "swift-direct-abstract")
}
#endif
