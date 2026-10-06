import KeywordMemberNames
import Testing

@Test
func swiftImplementationOfKeywordNamedInterfaceFunction() throws {
    class SwiftInit: Base, InitFunction {
        func `init`() -> String { "swift init" }
    }

    #expect(callInit(value: SwiftInit()) == "swift init")
}

@Test
func swiftImplementationOfKeywordNamedVarargInterfaceFunction() throws {
    class SwiftSelf: Base, SelfVarargFunction {
        func `self`(values: Int32...) -> Int32 { values.reduce(0, +) }
    }

    #expect(callSelf(value: SwiftSelf()) == 6)
}

@Test
func swiftImplementationOfKeywordNamedInterfaceProperty() throws {
    class SwiftInitProperty: Base, InitProperty {
        var `init`: String = "initial"
    }

    let value = SwiftInitProperty()
    #expect(getInit(value: value) == "initial")
    setInit(value: value, newValue: "updated")
    #expect(value.`init` == "updated")
    #expect(getInit(value: value) == "updated")
}

@Test
func swiftOverrideOfKeywordNamedOpenFunction() throws {
    class SwiftOpenInit: OpenInit {
        override func `init`() -> String { "swift" }
    }

    #expect(callOpenInit(value: OpenInit()) == "kotlin")
    #expect(callOpenInit(value: SwiftOpenInit()) == "swift")
}
