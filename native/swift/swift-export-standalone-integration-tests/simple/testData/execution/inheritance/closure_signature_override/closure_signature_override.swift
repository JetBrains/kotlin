import Inheritance
import Testing

@Test
func swiftCanOverrideKotlinMethodTakingClosure() throws {
    // The closure Kotlin passes in `callApply` reaches the Swift override as a Swift closure, and calling
    // it from Swift dispatches back into Kotlin.
    class Doubling: Transformer {
        override func apply(input: Int32, transform: @escaping (Int32) -> Int32) -> Int32 {
            return transform(input) * 2
        }
    }

    #expect(callApply(t: Doubling(), input: 4) == 80)
    // The Kotlin implementation is untouched.
    #expect(callApply(t: Transformer(), input: 4) == 41)
    // Direct Swift dispatch, with a Swift-made closure.
    #expect(Doubling().apply(input: 3, transform: { $0 + 1 }) == 8)
}

@Test
func swiftCanOverrideKotlinMethodReturningClosure() throws {
    // A closure produced by Swift travels into Kotlin, which stores and invokes it.
    class Multiplying: Transformer {
        override func makeAdder(addend: Int32) -> (Int32) -> Int32 {
            return { $0 * addend }
        }
    }

    #expect(callMakeAdder(t: Multiplying(), addend: 3, input: 5) == 15)
    #expect(callMakeAdder(t: Transformer(), addend: 3, input: 5) == 8)
}

@Test
func swiftOverrideOfMethodTakingClosureTakingClosure() throws {
    class Nested: Transformer {
        override func applyTwice(input: Int32, transform: @escaping (@escaping (Int32) -> Int32) -> Int32) -> Int32 {
            return transform({ $0 + 1 }) - input
        }
    }

    // Kotlin passes `{ f -> f(3) }`; the Swift override calls it with its own `{ $0 + 1 }`.
    #expect(callApplyTwice(t: Nested(), input: 10) == -6)
    #expect(callApplyTwice(t: Transformer(), input: 10) == 16)
}

@Test
func swiftOverrideOfMethodWithClosureOverReferenceTypes() throws {
    class Bracketing: Transformer {
        override func describe(name: String, format: @escaping (String) -> String) -> String {
            return format(name) + "!"
        }
    }

    #expect(callDescribe(t: Bracketing(), name: "kotlin") == "[kotlin]!")
    #expect(callDescribe(t: Transformer(), name: "kotlin") == "[kotlin]")
}

@Test
func swiftOverrideOfMethodWithVoidClosure() throws {
    class Repeating: Transformer {
        override func runAction(action: @escaping () -> Void) -> Void {
            action()
            action()
        }
    }

    actionRuns = 0
    callRunAction(t: Repeating())
    #expect(actionRuns == 2)

    actionRuns = 0
    callRunAction(t: Transformer())
    #expect(actionRuns == 1)
}

@Test
func swiftOverrideOfMethodWithOptionalClosure() throws {
    class Optionally: Transformer {
        override func applyOptional(input: Int32, transform: ((Int32) -> Int32)?) -> Int32 {
            guard let transform else { return -2 }
            return transform(input) + 1
        }
    }

    #expect(callApplyOptional(t: Optionally(), input: 1, pass: true) == 102)
    #expect(callApplyOptional(t: Optionally(), input: 1, pass: false) == -2)
    #expect(callApplyOptional(t: Transformer(), input: 1, pass: true) == 101)
    #expect(callApplyOptional(t: Transformer(), input: 1, pass: false) == -1)
}

@Test
func swiftOnlyImplementationOfInterfaceWithClosures() throws {
    // A pure Swift conformer: Kotlin-side dispatch can only reach it through reverse bridges.
    class SwiftProducer: ProducerBase, Producer {
        func produce() -> () -> String {
            return { "from swift" }
        }

        func consume(block: @escaping (String) -> Void) {
            block("swift value")
        }
    }

    let producer = SwiftProducer()
    #expect(callProduce(p: producer) == "from swift")
    callConsume(p: producer, value: "ignored")
    #expect(lastConsumed == "swift value")
}
