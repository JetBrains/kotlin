import Inheritance
import KotlinStdlib
import Testing


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

@Test
func kotlinThrowsSwiftBackedExceptionSubclass() throws {
    // The thrown object is a *Swift* subclass of an exported Kotlin
    // `Exception`.
    class SwiftThrowableLeaf: ThrowableBranch {
        override func throwableValue() -> String { "swift>" + super.throwableValue() }
    }

    let value = SwiftThrowableLeaf(origin: "primary")

    do {
        try throwProvided(value: value)
        Issue.record("expected throwProvided to throw")
    } catch let error as SwiftThrowableLeaf {
        #expect(error === value)
    } catch {
        Issue.record("expected the original SwiftThrowableLeaf, got \(type(of: error))")
    }
}
