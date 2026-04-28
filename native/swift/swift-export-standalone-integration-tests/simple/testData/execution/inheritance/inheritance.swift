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
