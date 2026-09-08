import Inheritance
import Testing

@Test
func twoSwiftSubclassLevelsPreserveSuperChainThroughKotlin() throws {
    class SwiftMiddle: BaseClass {
        override func baseFunction() -> String { "swift-middle>" + super.baseFunction() }
    }
    class SwiftLeaf: SwiftMiddle {
        override func baseFunction() -> String { "swift-leaf>" + super.baseFunction() }
    }

    let value = SwiftLeaf()
    #expect(value.baseFunction() == "swift-leaf>swift-middle>base-class")
    #expect(callFunBaseClass(value: value) == "swift-leaf>swift-middle>base-class")
}

@Test
func severalSwiftLevelsCallKotlinSuperWhichCallsBackToSwift() throws {
    class SwiftMiddle: SuperReentryBase {
        override func callback() -> String { "swift-middle-callback" }
        override func operation() -> String { "swift-middle>" + super.operation() }
    }

    class SwiftLeaf: SwiftMiddle {
        override func callback() -> String { "swift-leaf-callback" }
        override func operation() -> String { "swift-leaf>" + super.operation() }
    }

    let value = SwiftLeaf()
    #expect(value.operation() == "swift-leaf>swift-middle>kotlin-operation>swift-leaf-callback")
    #expect(callSuperReentry(value: value) == "swift-leaf>swift-middle>kotlin-operation>swift-leaf-callback")
}
