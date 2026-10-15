import Inheritance
import Testing
import KotlinRuntime

// KT-88251 — `KotlinBase.init()` is an available designated initializer, so a Swift class may inherit
// `KotlinBase` directly and still be a full participant of the Kotlin object graph.

@Test
func kotlinBaseIsInstantiableOnItsOwn() throws {
    // The counterpart of `KotlinBase` itself is a plain `kotlin.Any`.
    let base = KotlinBase()
    #expect(echoAny(value: base) as AnyObject === base)
    #expect(areSame(lhs: base, rhs: base))
}

@Test
func swiftClassInheritsKotlinBaseDirectly() throws {
    class Plain: KotlinBase {}

    let value = Plain()
    // Crosses the bridge as `Any` and comes back as the very same Swift instance.
    #expect(echoAny(value: value) as AnyObject === value)
    #expect(areSame(lhs: value, rhs: value))

    let storage = AnyStorage()
    storage.store(value: value)
    #expect(try #require(storage.retrieve()) as AnyObject === value)
}

@Test
func swiftClassInheritingKotlinBaseGetsItsOwnInitializer() throws {
    class Configured: KotlinBase {
        let label: String
        init(label: String) {
            self.label = label
            super.init()
        }
    }

    let value = Configured(label: "configured")
    #expect(value.label == "configured")
    #expect(echoAny(value: value) as AnyObject === value)
}

// The KT-88251 reproducer proper: no exported Kotlin class anywhere in the hierarchy.
@Test
func swiftOnlyImplementationOfKotlinInterface() throws {
    final class SwiftOnlySpeaker: KotlinBase & Speaker {
        func speak() -> String { "swift-only speaks" }
        func volume() -> Int32 { 3 }
    }

    let s = SwiftOnlySpeaker()

    // Direct Swift dispatch
    #expect(s.speak() == "swift-only speaks")
    #expect(s.volume() == 3)

    // Kotlin-side interface dispatch reaches the Swift implementations
    #expect(callSpeak(s: s) == "swift-only speaks")
    #expect(callVolume(s: s) == 3)

    // ...and the object keeps its identity across the boundary
    #expect(echoSpeaker(s: s) as AnyObject === s)
}

@Test
func swiftOnlyConformerAdoptsMultipleKotlinInterfaces() throws {
    final class SwiftOnlyIo: KotlinBase & Reader & Writer {
        func read() -> String { "swift-read" }
        func write(value: String) -> String { "swift-wrote-\(value)" }
    }

    let io = SwiftOnlyIo()
    #expect(callRead(r: io) == "swift-read")
    #expect(callWrite(w: io, value: "payload") == "swift-wrote-payload")
}

@Test
func swiftOnlyConformerInheritsKotlinInterfaceDefault() throws {
    // `describe()` is defaulted in Kotlin and calls the abstract `tag()`, which only Swift implements.
    final class SwiftOnlyDefaulter: KotlinBase & Defaulter {
        func tag() -> String { "swift-tag" }
    }

    let d = SwiftOnlyDefaulter()
    #expect(d.describe() == "defaulted(swift-tag)")
    #expect(callDescribe(d: d) == "defaulted(swift-tag)")
}

@Test
func swiftOnlyConformerOverridesKotlinInterfaceDefault() throws {
    final class SwiftOnlyOverrider: KotlinBase & Defaulter {
        func tag() -> String { "ignored" }
        func describe() -> String { "swift-describe" }
    }

    let d = SwiftOnlyOverrider()
    #expect(d.describe() == "swift-describe")
    #expect(callDescribe(d: d) == "swift-describe")
}

@Test
func secondSwiftLevelBelowKotlinBaseDispatchesToTheLeaf() throws {
    class SpeakerRoot: KotlinBase & Speaker {
        func speak() -> String { "root speaks" }
        func volume() -> Int32 { 1 }
    }
    class SpeakerLeaf: SpeakerRoot {
        override func speak() -> String { "leaf speaks" }
    }

    let root = SpeakerRoot()
    let leaf = SpeakerLeaf()

    #expect(callSpeak(s: root) == "root speaks")
    // The leaf gets its own synthesized Kotlin type, so Kotlin dispatch lands on the override...
    #expect(callSpeak(s: leaf) == "leaf speaks")
    // ...while the member it does not override still resolves to the root implementation.
    #expect(callVolume(s: leaf) == 1)
    #expect(echoSpeaker(s: leaf) as AnyObject === leaf)
}

@Test
func distinctSwiftInstancesStayDistinctInKotlin() throws {
    class Plain: KotlinBase {}

    let one = Plain()
    let two = Plain()

    #expect(!areSame(lhs: one, rhs: two))
    #expect(areSame(lhs: one, rhs: one))
    // A directly-KotlinBase-rooted counterpart has `kotlin.Any` semantics: equality is identity.
    #expect(areEqual(lhs: one, rhs: one))
    #expect(!areEqual(lhs: one, rhs: two))
    // Swift-side hashing agrees with the Kotlin `hashCode` of the counterpart.
    #expect(one.hash == Int(hashOf(value: one)))
}
