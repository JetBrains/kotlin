import Inheritance
import KotlinStdlib
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

@Test
func swiftOverrideCanCallSuperOnKotlinClass() throws {
    // A Swift override that calls `super.method()` must reach the inherited Kotlin implementation
    // via the non-virtual ("direct dispatch") forward bridge, instead of re-entering the patched
    // vtable slot and recursing forever.
    class FancyVehicle: Vehicle {
        override func describe() -> String {
            return "fancy-" + super.describe()
        }
    }
    let v = FancyVehicle()

    // Direct Swift dispatch: the override runs and its super-call lands in Kotlin.
    #expect(v.describe() == "fancy-kotlin-vehicle")
    // Kotlin-side dispatch reaches the Swift override, whose super-call again lands in Kotlin.
    #expect(callDescribe(v: v) == "fancy-kotlin-vehicle")

    // Original Kotlin instance is unaffected.
    #expect(callDescribe(v: Vehicle()) == "kotlin-vehicle")
    #expect(Vehicle().describe() == "kotlin-vehicle")
}

@Test
func swiftSubclassInheritsNonOverriddenKotlinMethod() throws {
    // A Swift subclass that overrides only some methods must still be able to invoke the
    // non-overridden ones (whose vtable slots are also patched) without infinite recursion.
    class FancyVehicle: Vehicle {
        override func describe() -> String { "fancy" }
        // `wheels()` is intentionally not overridden.
    }
    let v = FancyVehicle()

    #expect(v.describe() == "fancy")
    // Inherited, non-overridden method via direct Swift dispatch.
    #expect(v.wheels() == 4)
    // Inherited, non-overridden method reached through a Kotlin caller must not recurse.
    #expect(callWheels(v: v) == 4)
    #expect(callDescribe(v: v) == "fancy")
}

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

    // Kotlin-side interface dispatch lands in the Swift implementations
    #expect(callSpeak(s: s) == "swift base speaks")
    #expect(callVolume(s: s) == 7)
}

@Test
func swiftInheritsKotlinInterfaceDefault() throws {
    // Swift inherits a Kotlin class and first-adopts `Defaulter`, without overriding the defaulted
    // method `describe`. It must inherit Kotlin's default, dispatched non-virtually so it never
    // recurses through the patched itable. The default's open self-call to `tag()` reaches the Swift override.
    class MyDefaulter: Base, Defaulter {
        func tag() -> String { "swift-tag" }
        // `describe()` intentionally NOT overridden -> inherits the Kotlin default.
    }
    let d = MyDefaulter()

    // Direct Swift dispatch: inherited default runs; its open self-call reaches the Swift override.
    #expect(d.describe() == "default-describe(swift-tag)")
    #expect(d.tag() == "swift-tag")

    // Kotlin-side dispatch must terminate (no infinite recursion) and yield the same result.
    #expect(callDefDescribe(d: d) == "default-describe(swift-tag)")
    #expect(callDefTag(d: d) == "swift-tag")
}

@Test
func swiftOverridesKotlinInterfaceDefault() throws {
    // When Swift DOES override the defaulted method, its override must win both directly and via Kotlin.
    class MyDefaulter2: Base, Defaulter {
        func tag() -> String { "t2" }
        func describe() -> String { "swift-describe(" + tag() + ")" }
    }
    let d = MyDefaulter2()
    #expect(d.describe() == "swift-describe(t2)")
    #expect(callDefDescribe(d: d) == "swift-describe(t2)")
}

@Test
func swiftCanOverrideKotlinInterfaceProperty() throws {
    // Swift subclass of a Kotlin class implementing a Kotlin interface overrides the interface's
    // settable property. Kotlin-side interface dispatch (setCount/getCount, typed as Counter) must
    // reach the Swift accessors via the patched itable — both getter and setter reverse bridges.
    class SwiftCounter: CounterBase {
        private var backing: Int32 = 0
        override var count: Int32 {
            get { backing }
            set { backing = newValue * 2 } // observable transform proves the Swift setter ran
        }
    }
    let c = SwiftCounter()
    setCount(c: c, n: 5)              // Kotlin -> Swift setter
    #expect(getCount(c: c) == 10)     // Kotlin -> Swift getter
    #expect(c.count == 10)            // direct Swift dispatch

    // Original Kotlin implementation untouched.
    let kotlin = CounterBase()
    setCount(c: kotlin, n: 3)
    #expect(getCount(c: kotlin) == 3)
}

@Test
func swiftCanOverrideKotlinClassProperty() throws {
    // Swift subclass overrides both a get-only (`val`) and a settable (`var`) property of an open
    // Kotlin class. Kotlin-side access must reach the Swift accessors via the patched vtable.
    class SwiftNamed: Named {
        override var label: String { "swift-label" } // override get-only `val`
        private var nickBacking = "swift-nick"
        override var nick: String {                    // override settable `var`
            get { nickBacking }
            set { nickBacking = "got:" + newValue }
        }
    }
    let n = SwiftNamed()

    // Direct Swift dispatch
    #expect(n.label == "swift-label")
    #expect(n.nick == "swift-nick")

    // Kotlin-side dispatch reaches the Swift accessors
    #expect(readLabel(n: n) == "swift-label")
    #expect(readNick(n: n) == "swift-nick")
    writeNick(n: n, v: "x")
    #expect(readNick(n: n) == "got:x")

    // Original Kotlin instance untouched
    let k = Named()
    #expect(readLabel(n: k) == "kotlin-label")
    writeNick(n: k, v: "y")
    #expect(readNick(n: k) == "y")
}

@Test
func swiftPropertySuperAndNonOverridden() throws {
    // Property analog of swiftOverrideCanCallSuperOnKotlinClass / swiftSubclassInheritsNonOverriddenKotlinMethod:
    // a Swift override reading `super.title` reaches the inherited Kotlin getter via the non-virtual
    // `_direct` bridge; `rank` is not overridden and must be reachable from Kotlin without recursing.
    class FancyBook: Book {
        override var title: String { "fancy-" + super.title }
        // `rank` intentionally not overridden.
    }
    let b = FancyBook()

    #expect(b.title == "fancy-kotlin-title")
    #expect(readTitle(b: b) == "fancy-kotlin-title")
    // Inherited, non-overridden property reached through Kotlin must not recurse.
    #expect(b.rank == 1)
    #expect(readRank(b: b) == 1)

    #expect(readTitle(b: Book()) == "kotlin-title")
}

@Test
func swiftInheritsKotlinInterfaceDefaultProperty() throws {
    // Swift inherits a Kotlin class and first-adopts `Labeled` without overriding the defaulted
    // property `display`. It must inherit Kotlin's default (dispatched non-virtually so it never
    // recurses through the patched itable); the default's open self-call to `base` reaches the Swift override.
    class MyLabeled: Base, Labeled {
        var base: String { "swift-base" }
        // `display` intentionally NOT overridden -> inherits the Kotlin default.
    }
    let l = MyLabeled()

    #expect(l.display == "display(swift-base)")
    #expect(readDisplay(l: l) == "display(swift-base)")
    #expect(readBase(l: l) == "swift-base")
}

@Test
func swiftOverridesKotlinInterfaceDefaultProperty() throws {
    // When Swift DOES override the defaulted property, its override must win both directly and via Kotlin.
    class MyLabeled2: Base, Labeled {
        var base: String { "b2" }
        var display: String { "swift-display(" + base + ")" }
    }
    let l = MyLabeled2()
    #expect(l.display == "swift-display(b2)")
    #expect(readDisplay(l: l) == "swift-display(b2)")
}

enum MySwiftError: Error {
    case boom(String)
}

@Test
func swiftReverseThrowSurfacesToKotlin() throws {
    // A Swift override of a @Throws Kotlin method throws a Swift error; a Kotlin caller must observe
    // a thrown exception (not a trap), with the message preserved.
    class BadThrower: Thrower {
        override func mightThrow(prefix: String) throws -> String {
            throw MySwiftError.boom("swift-boom:" + prefix)
        }
    }
    #expect(callMightThrowCatching(t: BadThrower(), prefix: "p").contains("swift-boom:p"))

    // A Swift override that does NOT throw still returns normally through the reverse bridge.
    class GoodThrower: Thrower {
        override func mightThrow(prefix: String) throws -> String { prefix + "-swift-ok" }
    }
    #expect(callMightThrowCatching(t: GoodThrower(), prefix: "p") == "ok:p-swift-ok")

    // Original Kotlin implementation unaffected.
    #expect(callMightThrowCatching(t: Thrower(), prefix: "p") == "ok:p-kotlin-ok")
}

@Test
func kotlinExceptionRoundTripsThroughSwiftOverride() throws {
    // A Swift override calls `super` (a @Throws Kotlin method that
    // throws). Letting it propagate, the Kotlin caller must receive the ORIGINAL Kotlin exception —
    // identity and message preserved — not a generic re-wrap.
    class Propagator: SuperThrower {
        override func boom() throws -> String {
            return try super.boom() // Kotlin throws MyKotlinException("kotlin-boom")
        }
    }
    #expect(callBoomCatching(s: Propagator()) == "kotlin-exception:kotlin-boom")
    // Original Kotlin instance also yields the Kotlin exception.
    #expect(callBoomCatching(s: SuperThrower()) == "kotlin-exception:kotlin-boom")
}

@Test
func swiftErrorRoundTripsBackToSwift() throws {
    // A Swift override throws a Swift error; a @Throws Kotlin relay propagates it back
    // out to Swift, where it must arrive as the SAME Swift error type/value (forward SwiftError unwrap).
    class ThrowingRelayer: Relayer {
        override func relay() throws -> String { throw MySwiftError.boom("round-trip") }
    }
    do {
        _ = try callRelay(r: ThrowingRelayer())
        Issue.record("expected callRelay to throw")
    } catch let error as MySwiftError {
        guard case .boom(let message) = error else {
            Issue.record("unexpected MySwiftError case")
            return
        }
        #expect(message == "round-trip")
    }
}

@Test
func kotlinExceptionThrownBySwiftOverrideSurfacesToSwift() throws {
    // A Swift override throws a *Kotlin* exception object it constructed itself. It crosses into Kotlin via the
    // reverse bridge — where `kotlinThrowableRCRef` must hand over the original throwable instead of boxing it in
    // a `SwiftError` — the @Throws Kotlin relay propagates it, and the forward bridge must deliver it back to
    // Swift as the concrete exported `MyKotlinException`: not a `KotlinError` fallback, not a `SwiftError` re-wrap.
    class KotlinErrorRelayer: Relayer {
        override func relay() throws -> String {
            throw MyKotlinException(message: "swift-thrown-kotlin-error")
        }
    }
    do {
        _ = try callRelay(r: KotlinErrorRelayer())
        Issue.record("expected MyKotlinException to be thrown")
    } catch let error as MyKotlinException {
        #expect(error.message == "swift-thrown-kotlin-error")
    } catch {
        Issue.record("expected MyKotlinException, got \(type(of: error)): \(error)")
    }
}

// A Kotlin type that is not a `Throwable` may still be thrown from Swift once it is retroactively conformed to
// `Swift.Error`. Kotlin cannot rethrow such an object as-is, so the reverse bridge must box it into a `SwiftError`
// rather than hand its ref over as a throwable (which used to produce a `ClassCastException` in Kotlin).
extension NotAThrowable: @retroactive Error {}

@Test
func retroactivelyConformedKotlinErrorRoundTripsBackToSwift() throws {
    let thrown = NotAThrowable(tag: "retroactive")
    class RetroRelayer: Relayer {
        let error: NotAThrowable
        init(error: NotAThrowable) {
            self.error = error
            super.init()
        }
        override func relay() throws -> String { throw error }
    }
    do {
        _ = try callRelay(r: RetroRelayer(error: thrown))
        Issue.record("expected NotAThrowable to be thrown")
    } catch let error as NotAThrowable {
        #expect(error === thrown, "the very same Kotlin instance must come back to Swift")
        #expect(error.tag == "retroactive")
    } catch {
        Issue.record("expected NotAThrowable, got \(type(of: error)): \(error)")
    }
}

@Test
func retroactivelyConformedKotlinErrorSurfacesToKotlinAsSwiftError() throws {
    // Seen from Kotlin, a non-throwable Swift error is an ordinary boxed `SwiftError` whose message is the
    // object's description — NOT a `ClassCastException` from an illegal `as kotlin.Throwable` on the way in.
    class RetroThrower: Thrower {
        override func mightThrow(prefix: String) throws -> String { throw NotAThrowable(tag: prefix) }
    }
    let observed = callMightThrowCatching(t: RetroThrower(), prefix: "p")
    #expect(observed.hasPrefix("throwable:"), "Kotlin must observe a thrown exception, got \(observed)")
    #expect(observed.contains("NotAThrowable"), "the message must describe the thrown object, got \(observed)")
    #expect(!observed.contains("cannot be cast"), "the object must not be force-cast to Throwable, got \(observed)")
}
