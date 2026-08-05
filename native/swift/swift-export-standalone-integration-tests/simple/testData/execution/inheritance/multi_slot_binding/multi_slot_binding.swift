import Inheritance
import Testing

// One Swift implementation that has to be installed into *two* itable places. Swift Export emits one
// @BindReverseBridgeToMethod per slot and the annotation is @Repeatable precisely for this shape, so these
// are the only tests that can drive the multi-binding path: everything else needs a single slot.

@Test
func swiftImplementationResolvesConflictingInterfaceDefaults() throws {
    class SwiftConflict: ConflictAnchor, LeftDefault, RightDefault {
        func conflict() -> String { "swift-conflict" }
    }

    let value = SwiftConflict()
    #expect(callLeftDefault(value: value) == "swift-conflict")
    #expect(callRightDefault(value: value) == "swift-conflict")
}

@Test
func typeRichPropertySuperDispatchesThroughInterfaceView() throws {
    class SwiftRichSuper: TypeRichSuperBase {
        override var richData: DataPayload {
            let inherited = super.richData
            return DataPayload(text: "swift>\(inherited.text)", number: inherited.number + 2)
        }
    }

    let value = SwiftRichSuper()
    let interfaceView = richDataAsView(value: value)
    let mirrorView = richDataAsMirrorView(value: value)
    let result = readRichData(value: interfaceView)
    #expect(interfaceView === value)
    #expect(mirrorView === value)
    #expect(result.text == "swift>kotlin-super")
    #expect(result.number == 42)
    #expect(readMirroredRichData(value: mirrorView).text == "swift>kotlin-super")
}
