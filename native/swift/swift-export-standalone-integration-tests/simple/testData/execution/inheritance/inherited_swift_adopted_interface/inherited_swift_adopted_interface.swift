import Inheritance
import Testing

// KT-88042. Note that this harness compiles the generated bridges into the root module with Kotlin/Native caches
// disabled, so it does not cover the second, unrelated half of the issue: swift-export type adapters being
// dropped when the generated-bridges klib is compiled into a cache, which is what made the *direct* subclass
// case fail in Debug configurations too.

// Shared by the two tests below. File scope, not function-local, because both a non-overriding and an
// overriding leaf are derived from it.
private class MidSpeaker: Base, Speaker {
    func speak() -> String { "mid speaks" }
    func volume() -> Int32 { 3 }
}

@Test
func swiftGrandchildInheritsKotlinInterfaceConformance() throws {
    // The leaf declares nothing, so `class_copyProtocolList(LeafSpeaker)` is empty and the `_Speaker` marker
    // lives on `MidSpeaker`. Without walking the superclass chain the synthesized TypeInfo carries no
    // interfaces and every Kotlin-side Speaker check fails.
    class LeafSpeaker: MidSpeaker {} // intentionally adds nothing

    let leaf = LeafSpeaker()

    // Pure Swift dispatch — worked before the fix too.
    #expect(leaf.speak() == "mid speaks")

    // The runtime type check itself: was `false` / ClassCastException before the fix.
    #expect(isSpeaker(a: leaf))
    #expect(castAndSpeak(a: leaf) == "mid speaks")

    // Kotlin-side interface dispatch.
    #expect(callSpeak(s: leaf) == "mid speaks")
    #expect(callVolume(s: leaf) == 3)

    // The class-derived slot on the same object still resolves.
    #expect(callGreet(base: leaf) == "Hello from Kotlin")

    // The direct-subclass case must keep working.
    #expect(callSpeak(s: MidSpeaker()) == "mid speaks")
}

@Test
func swiftGrandchildOverridingInheritedInterfaceMethod() throws {
    // Same chain, but the leaf overrides. The itable slot inherited through the discovered conformance holds
    // the Swift reverse trampoline, so Kotlin dispatch must land on the leaf's override rather than on the
    // intermediate class's implementation.
    class LoudLeaf: MidSpeaker {
        override func speak() -> String { "leaf speaks" }
        override func volume() -> Int32 { 9 }
    }

    let loud = LoudLeaf()
    #expect(loud.speak() == "leaf speaks")
    #expect(callSpeak(s: loud) == "leaf speaks")
    #expect(callVolume(s: loud) == 9)
}

@Test
func swiftGrandchildAddsItsOwnKotlinInterfaceConformance() throws {
    // The *union* of the conformances across the chain has to be taken: `Reader` comes from the intermediate
    // class, `Writer` from the leaf. Collecting from the leaf alone would drop `Reader`.
    class MidReader: Base, Reader {
        func read() -> String { "mid reads" }
    }
    class LeafWriter: MidReader, Writer {
        func write(s: String) -> Int32 { Int32(s.count) }
    }

    let lw = LeafWriter()
    #expect(callRead(r: lw) == "mid reads")
    #expect(callWrite(w: lw, s: "abcd") == 4)

    // Four levels: proves the walk is not bounded to a single step.
    class LeafLeaf: LeafWriter {}
    let ll = LeafLeaf()
    #expect(callRead(r: ll) == "mid reads")
    #expect(callWrite(w: ll, s: "abcd") == 4)
}

@Test
func swiftGrandchildInheritsRefinedInterfaceConformance() throws {
    // Protocol *refinement* walking (`_Dog` refines `_Animal`) must still happen when the worklist is seeded
    // from a superclass rather than from the leaf, so a Kotlin caller typed as the parent interface reaches
    // the Swift implementation.
    class MidDog: Base, Dog {
        func name() -> String { "mid-dog" }
        func bark() -> String { "mid-woof" }
    }
    class LeafDog: MidDog {}

    let leaf = LeafDog()
    #expect(callName(a: leaf) == "mid-dog")
    #expect(callBark(d: leaf) == "mid-woof")
}

@Test
func swiftGrandchildInheritsKotlinInterfaceProperty() throws {
    // Property accessors are ordinary itable entries, so an inherited conformance has to route the getter and
    // the setter to the intermediate class's implementations as well.
    class MidCounter: Base, Counter {
        private var backing: Int32 = 0
        var count: Int32 {
            get { backing }
            set { backing = newValue * 2 } // observable transform proves the Swift setter ran
        }
    }
    class LeafCounter: MidCounter {}

    let c = LeafCounter()
    setCount(c: c, n: 5)          // Kotlin -> Swift setter
    #expect(getCount(c: c) == 10) // Kotlin -> Swift getter
    #expect(c.count == 10)        // direct Swift dispatch
}
