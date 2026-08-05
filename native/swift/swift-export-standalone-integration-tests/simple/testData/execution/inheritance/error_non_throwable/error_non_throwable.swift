import Inheritance
import KotlinStdlib
import Testing

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
