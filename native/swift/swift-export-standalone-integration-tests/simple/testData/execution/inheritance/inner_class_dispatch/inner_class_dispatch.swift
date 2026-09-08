import Inheritance
import Testing

@Test
func innerClassThisCallReachesSwiftOverride() throws {
    class SwiftOuter: InnerOuter {
        override func value() -> String { "swift-outer" }
    }
    #expect(callInnerViaThis(o: SwiftOuter()) == "swift-outer")
    // The plain Kotlin instance is unaffected.
    #expect(callInnerViaThis(o: InnerOuter()) == "kotlin-outer")
}

@Test
func innerClassSuperOuterCallStaysInKotlin() throws {
    class SwiftOuter: InnerOuter {
        override func value() -> String { "swift-outer" }
    }
    #expect(callInnerViaSuperOuter(o: SwiftOuter()) == "kotlin-base")
    #expect(callInnerViaSuperOuter(o: InnerOuter()) == "kotlin-base")
}
