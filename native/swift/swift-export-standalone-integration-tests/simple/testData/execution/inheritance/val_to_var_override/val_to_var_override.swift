import Inheritance
import Testing

// The mutable property is declared in Kotlin
@Test
func kotlinVarOverridesKotlinVal() throws {
    let derived = ValToVarDerived()

    // Reading through the subclass must not be ambiguous, and must see the override.
    #expect(derived.valToVar == "derived")

    // Reading through the superclass must dispatch to the override as well.
    #expect((derived as ValToVarBase).valToVar == "derived")
    #expect(readValToVar(base: derived) == "derived")

    derived.valToVar = "assigned"
    #expect(derived.valToVar == "assigned")
    #expect((derived as ValToVarBase).valToVar == "assigned")
    #expect(readValToVar(base: derived) == "assigned")

    // The plain Kotlin base class is unaffected.
    #expect(ValToVarBase().valToVar == "base")
}

// The mutable property is declared in Swift
@Test
func swiftVarOverridesKotlinVal() throws {
    class SwiftDerived: ValToVarBase {
        private var storage = "swift"
        override var valToVar: String {
            get { storage }
            set { storage = newValue }
        }
    }

    let derived = SwiftDerived()
    #expect(derived.valToVar == "swift")
    #expect(readValToVar(base: derived) == "swift")

    derived.valToVar = "swift-assigned"
    #expect(derived.valToVar == "swift-assigned")
    #expect(readValToVar(base: derived) == "swift-assigned")
}
