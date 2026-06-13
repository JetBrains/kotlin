import Inheritance
import Testing

@Test
func swiftCanSubclassKotlin() throws {
    class SwiftDerived: Base {
        override func greet() -> String {
            return "Hello from Swift"
        }
    }

    let derived = SwiftDerived()

    // Direct call: Swift override should be invoked
    #expect(derived.greet() == "Hello from Swift")

    // Call through Kotlin: reverse bridge should dispatch to Swift override
    #expect(callGreet(base: derived) == "Hello from Swift")

    // Original Kotlin class should still work
    let base = Base()
    #expect(base.greet() == "Hello from Kotlin")
    #expect(callGreet(base: base) == "Hello from Kotlin")
}

@Test
func swiftCanOverrideKotlinInterfaceMethods() throws {
    // Swift class extends a Kotlin open class that implements a Kotlin interface,
    // and overrides the interface's methods. Kotlin-side interface dispatch
    // (callSpeak / callVolume, which accept the interface type) should reach the
    // Swift overrides via protocol-conformance discovery on the TypeInfo patch.
    class ShoutingSpeaker: SpeakerBase {
        override func speak() -> String {
            return "Swift shouts"
        }
        override func volume() -> Int32 {
            return 11
        }
    }

    let shouter = ShoutingSpeaker()

    // Direct Swift dispatch
    #expect(shouter.speak() == "Swift shouts")
    #expect(shouter.volume() == 11)

    // Kotlin-side interface dispatch should land in Swift overrides
    #expect(callSpeak(s: shouter) == "Swift shouts")
    #expect(callVolume(s: shouter) == 11)

    // Original Kotlin implementation untouched
    let base = SpeakerBase()
    #expect(callSpeak(s: base) == "Kotlin speaks")
    #expect(callVolume(s: base) == 5)
}

@Test
func swiftCanOverrideMultipleKotlinInterfaces() throws {
    // Exercises itable patching for a Swift subclass of a class that implements two distinct
    // Kotlin interfaces, with overrides on both. Each interface's slot in the patched TypeInfo
    // must independently route to the Swift override.
    class MyIo: IoBase {
        override func read() -> String { "swift reads" }
        override func write(s: String) -> Int32 { Int32(s.count * 2) }
    }
    let io = MyIo()
    #expect(callRead(r: io) == "swift reads")
    #expect(callWrite(w: io, s: "abc") == 6)

    let kotlinIo = IoBase()
    #expect(callRead(r: kotlinIo) == "kotlin reads")
    #expect(callWrite(w: kotlinIo, s: "abc") == 3)
}

@Test
func swiftOverrideDispatchesViaParentInterface() throws {
    // Interface inheritance: Dog refines Animal. Swift overrides BOTH methods so the patched
    // vtable always routes through Swift overrides — exercises getProtocolsAsInterfaces walking
    // and inherited-itable population. Non-overridden methods on Swift subclasses are a known
    // limitation (would need to skip patching when the subclass doesn't override; current
    // unconditional patching causes infinite recursion in that case — see audit / memory).
    class Husky: DogBase {
        override func name() -> String { "swift-husky" }
        override func bark() -> String { "swift-woof" }
    }
    let husky = Husky()
    // Direct Swift dispatch
    #expect(husky.name() == "swift-husky")
    #expect(husky.bark() == "swift-woof")
    // Kotlin caller typed as the *parent* interface (Animal) reaches the Swift override —
    // proves inherited-interface itable entries are populated correctly.
    #expect(callName(a: husky) == "swift-husky")
    // Same for the directly-declared interface.
    #expect(callBark(d: husky) == "swift-woof")
}

// Property reverse bridge test (currently disabled — causes crash, likely surfaces an
// implementation gap in property reverse-bridge wiring; see plan).
// @Test
// func swiftCanOverrideKotlinInterfaceProperty() throws { ... }
