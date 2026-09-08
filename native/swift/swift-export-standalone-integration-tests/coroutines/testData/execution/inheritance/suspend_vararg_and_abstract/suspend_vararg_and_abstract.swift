import Main
import Testing
import Foundation

// A Swift override of a Kotlin `suspend` method with a `vararg` parameter. The reverse async bridge
// must call the variadic Swift method with the runtime array it received from Kotlin.
@Test
func swiftCanOverrideKotlinSuspendVarargMethod() async throws {
    class SwiftVararg: AsyncVararg {
        override func join(parts: String...) async throws -> String {
            return "Swift: " + parts.joined(separator: ",")
        }
    }

    #expect(try await callJoin(v: SwiftVararg()) == "Swift: a,b,c")
    // The original Kotlin class is untouched.
    #expect(try await callJoin(v: AsyncVararg()) == "Kotlin: a,b,c")
}


// Blocked by KT-87947
// `.disabled(...)` cannot express this — a disabled test is still compiled — so the body is parked behind an
// inactive `#if`, which Swift parses but never type-checks. Delete the `#if`/`#endif` once KT-87947 lands
#if KT87947_FIXED
@Test(.disabled("KT-87947: Swift can't inherit Kotlin abstract class"))
func swiftDirectSubclassOverridesAbstractKotlinSuspendMember() async throws {
    class SwiftAbstractVararg: AbstractAsyncVarargRoot {
        override func abstractJoin(parts: String...) async throws -> String {
            "Swift abstract: " + parts.joined(separator: ",")
        }
    }

    #expect(try await callAbstractJoin(value: SwiftAbstractVararg()) == "Swift abstract: a,b,c")
}
#endif
