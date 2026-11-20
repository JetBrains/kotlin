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


@Test
func swiftDirectSubclassOverridesAbstractKotlinSuspendMember() async throws {
    class SwiftAbstract: AsyncAbstractRoot {
        override func abstractGreet() async throws -> String { "swift-abstract" }
    }

    let value = SwiftAbstract()
    #expect(try await callAbstractGreet(value: value) == "swift-abstract")
    #expect(try await value.abstractGreet() == "swift-abstract")
    #expect(try await callConcreteGreet(value: value) == "kotlin-concrete")
    #expect(try await value.concreteGreet() == "kotlin-concrete")
}
