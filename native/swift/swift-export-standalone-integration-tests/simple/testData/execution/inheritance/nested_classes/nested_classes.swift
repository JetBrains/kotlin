import Inheritance
import Testing

@Test
func swiftCanSubclassNestedKotlinOpenClass() throws {
    class SwiftNestedLeaf: NestedInheritanceContainer.NestedBase {
        override func nestedValue() -> String { "swift-nested>" + super.nestedValue() }
    }

    #expect(callNestedValue(value: SwiftNestedLeaf()) == "swift-nested>kotlin-nested")
}

@Test
func nestedPartialPropertyOverrideThroughInterfaceView() throws {
    class SwiftNestedRichLeaf: NestedInheritanceContainer.NestedBase {
        override var nestedData: DataPayload {
            DataPayload(text: "swift>" + super.nestedData.text, number: super.nestedData.number + 1)
        }
        // nestedMode is intentionally inherited.
    }

    let interfaceView = nestedAsRichView(value: SwiftNestedRichLeaf())
    #expect(readNestedData(value: interfaceView).text == "swift>kotlin-nested-data")
    #expect(readNestedData(value: interfaceView).number == 51)
    #expect(readNestedMode(value: interfaceView) == .kotlinMode)
}
