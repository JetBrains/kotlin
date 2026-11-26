import Inheritance
import Testing

@Test
func concreteOverrideIsDispatchedThroughGenericSuperclass() throws {
    let b: GenericBase = ConcreteDerived()
    #expect(b.get() as? String == "derived")
    #expect(b.run() as? String == "overridden_abstract")
    #expect(b.put(value: "x") == "derived-put(x)")
}

@Test
func concreteOverrideIsCallableOnTheSubclass() throws {
    let d = ConcreteDerived()
    let got: String = d.get()
    let ran: String = d.run()
    #expect(got == "derived")
    #expect(ran == "overridden_abstract")
    #expect(d.put(value: "x") == "derived-put(x)")
}

@Test
func kotlinDispatchReachesConcreteOverride() throws {
    #expect(callGet(b: ConcreteDerived()) as? String == "derived")
    #expect(callRun(b: ConcreteDerived()) as? String == "overridden_abstract")
}

@Test
func swiftSubclassOfConcreteDerivedInheritsKotlinOverride() throws {
    class Inheriting: OpenConcreteDerived {}

    let b: GenericBase = Inheriting()
    #expect(b.get() as? String == "open-derived")
    #expect(b.run() as? String == "open-run")
    #expect(callGet(b: Inheriting()) as? String == "open-derived")
}

@Test
func swiftOverrideOfConcreteOverrideIsDispatchedEverywhere() throws {
    class Overriding: OpenConcreteDerived {
        override func get() -> String { "swift" }
    }

    let b: GenericBase = Overriding()
    #expect(b.get() as? String == "swift")
    #expect(callGet(b: Overriding()) as? String == "swift")
    let got: String = Overriding().get()
    #expect(got == "swift")
}
