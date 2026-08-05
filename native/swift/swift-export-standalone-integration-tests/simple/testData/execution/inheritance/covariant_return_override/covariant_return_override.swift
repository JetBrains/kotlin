import Inheritance
import Testing

// The narrowed type is declared in Kotlin
@Test
func swiftOverrideNarrowsReturnTypeToKotlinSubclass() throws {
    class SwiftFactory: ReturnFactory {
        override func make() -> ReturnDerived { ReturnDerived() }
    }

    let f = SwiftFactory()
    #expect(callMakeTag(f: f) == "kotlin-derived")
    #expect(callMake(f: f) is ReturnDerived)

    // The plain Kotlin factory is unaffected.
    #expect(callMakeTag(f: ReturnFactory()) == "kotlin-base")
}

// The narrowed type is declared in Swift
@Test
func swiftOverrideNarrowsReturnTypeToSwiftSubclass() throws {
    final class SwiftMade: ReturnBase {
        override func tag() -> String { "swift-made" }
    }
    class SwiftFactory: ReturnFactory {
        override func make() -> SwiftMade { SwiftMade() }
    }

    let f = SwiftFactory()
    #expect(callMakeTag(f: f) == "swift-made")

    let returned = callMake(f: f)
    #expect(returned is SwiftMade)
    #expect(returned.tag() == "swift-made")
}
