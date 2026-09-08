import Inheritance
import Testing
import KotlinRuntime

@Test
func swiftSubclassOfKotlinClassConformsToUnrelatedKotlinInterface() throws {
    // Regression for the implementation-marker fix: a Swift class subclasses an exported Kotlin
    // class (Base) whose Kotlin type does NOT implement Speaker, and separately conforms to the
    // Kotlin interface Speaker. Previously the witness extension was constrained on
    // _KotlinBridgeable (which every KotlinBase subclass satisfies), so Swift would silently supply
    // a delegating default that calls Speaker_speak on the Base backing — which doesn't implement
    // Speaker — yielding wrong dispatch / a crash. Now the witness is gated on the implementation
    // marker `__Speaker`, which Base does not carry, so the Swift class must implement the interface
    // itself, and Kotlin-side interface dispatch reaches those Swift implementations.
    class SpeakingBase: Base, Speaker {
        func speak() -> String { "swift base speaks" }
        func volume() -> Int32 { 7 }
    }

    let s = SpeakingBase()

    // Direct Swift dispatch
    #expect(s.speak() == "swift base speaks")
    #expect(s.volume() == 7)
    #expect(s.greet() == "Hello from Kotlin")

    // Kotlin-side interface dispatch lands in the Swift implementations
    #expect(callSpeak(s: s) == "swift base speaks")
    #expect(callVolume(s: s) == 7)
    #expect(callGreet(base: s) == "Hello from Kotlin")
}

// KT-88251 — a Swift class that implements a Kotlin interface with no exported Kotlin class in its
// hierarchy, written as `KotlinBase & <interface>`. It cannot be constructed today, because
// `KotlinBase.init()` is NS_UNAVAILABLE:
//
//   error: 'init()' is unavailable
//   KotlinRuntime/KotlinBase.h: note: 'init()' has been explicitly marked unavailable here
//
// `.disabled(...)` cannot be used here, because a disabled test is still compiled and this body does not
// compile. Delete the `#if`/`#endif` (and add `import KotlinRuntime`) once KT-88251 is
// fixed;
#if KT88251_FIXED
@Test(.disabled("KT-88251: cannot instantiate KotlinBase"))
func swiftOnlyImplementationOfKotlinInterface() throws {
    final class SwiftOnlySpeaker: KotlinBase & Speaker {
        func speak() -> String { "swift-only speaks" }
        func volume() -> Int32 { 3 }
    }

    let s = SwiftOnlySpeaker()
    #expect(callSpeak(s: s) == "swift-only speaks")
    #expect(callVolume(s: s) == 3)
}
#endif
