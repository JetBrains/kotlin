import Inheritance
import Testing

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
