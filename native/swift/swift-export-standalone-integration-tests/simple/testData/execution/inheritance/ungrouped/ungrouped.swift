import Inheritance
import Testing

@Test
func kotlinSafeCallsSeeSwiftNilOverride() throws {
    class SwiftNilTag: SideEffectBase {
        override var tag: Swift.String? { nil }
    }

    let value = SwiftNilTag()
    #expect(tagLength(value: value) == -1)
    #expect(tagOrDefault(value: value) == "kotlin-default")
}

@Test
func swiftOverrideBridgesObjectReferenceParameterAndResult() throws {
    class SwiftTypeRich: TypeRichBase {
        override func mapData(value: DataPayload) -> DataPayload {
            DataPayload(text: "swift:\(value.text)", number: value.number + 10)
        }
    }

    let result = callMapData(
        value: SwiftTypeRich(),
        payload: DataPayload(text: "payload", number: 2)
    )
    #expect(result.text == "swift:payload")
    #expect(result.number == 12)
}

@Test()
func swiftOverrideBridgesEnumParametersAndResults() throws {
    class SwiftTypeRich: TypeRichBase {
        override func mapEnum(value: InheritanceMode) -> InheritanceMode {
            value == .kotlinMode ? .swiftMode : .kotlinMode
        }
    }

    #expect(callMapEnum(value: SwiftTypeRich(), mode: .kotlinMode) == .swiftMode)
    #expect(callMapEnum(value: SwiftTypeRich(), mode: .swiftMode) == .kotlinMode)
}

@Test
func swiftOverrideBridgesInlineValueClassParametersAndResults() throws {
    class SwiftTypeRich: TypeRichBase {
        override func mapInline(value: InlinePayload) -> InlinePayload {
            InlinePayload(value: value.value + 100)
        }
    }

    let result = callMapInline(value: SwiftTypeRich(), payload: InlinePayload(value: 3))
    #expect(result.value == 103)
}
