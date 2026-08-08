import Inheritance
import Testing

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
