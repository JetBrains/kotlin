import Inheritance
import Testing

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
