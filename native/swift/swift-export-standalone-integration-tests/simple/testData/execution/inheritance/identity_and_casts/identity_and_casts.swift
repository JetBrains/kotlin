import Inheritance
import Testing

// Regression probe: an inherited interface member that Swift does NOT override must not be patched
// into a recursive reverse bridge.
@Test
func nonOverriddenParentInterfaceRequirementDoesNotRecurse() throws {
    class PartialSwiftRequirement: CompleteRequirementBase {
        override func childRequirement() -> String { "swift-child" }
        // parentRequirement is intentionally inherited from Kotlin.
    }

    let value = PartialSwiftRequirement()
    let storage = RequirementStorage()
    storage.store(value: value)
    let retrieved = try #require(storage.retrieve())
    let parentView: any ParentRequirement = retrieved
    let childView: any ChildRequirement = retrieved
    #expect(retrieved === value)
    #expect(parentView === value)
    #expect(childView === value)
    #expect(callChildRequirement(value: childView) == "swift-child")
    #expect(callParentRequirement(value: parentView) == "kotlin-parent")
}

@Test
func castsSurviveKotlinStorageAndRepeatedWrapperConversions() throws {
    class SwiftCastLeaf: CastBase {
        override func parentToken() -> String { "swift-parent-token" }
        override func childToken() -> String { "swift-child-token" }
    }

    let value = SwiftCastLeaf()
    let storage = CastStorage()
    storage.store(value: value)

    // Kotlin returns the object as the parent interface
    let storedParent = try #require(storage.retrieve())
    #expect(storedParent === value)
    #expect(callCastParent(value: storedParent) == "swift-parent-token")

    // Parent interface -> child interface -> exported Kotlin class.
    let childView = try #require(storedParent as? any CastChild)
    let classView = try #require(childView as? CastBase)
    #expect(childView === value)
    #expect(classView === value)
    #expect(callCastChild(value: childView) == "swift-child-token")

    // Repeated Kotlin round trips should not manufacture a different Swift wrapper.
    let parentAgain = echoCastParent(value: storedParent)
    let parentThird = echoCastParent(value: parentAgain)
    let classAgain = try #require(parentThird as? CastBase)
    #expect(parentAgain === value)
    #expect(parentThird === value)
    #expect(classAgain === value)
}

@Test
func kotlinObjectsStoredInSwiftSubclassFieldsRoundTripByIdentity() throws {
    class SwiftFieldOwner: FieldOwnerBase {
        let swiftStoredField: FieldPayload

        override init(initial: FieldPayload) {
            swiftStoredField = FieldPayload(label: "swift-only")
            super.init(initial: initial)
        }

        override func selected() -> FieldPayload { swiftStoredField }
    }

    let initial = FieldPayload(label: "initial")
    let value = SwiftFieldOwner(initial: initial)
    #expect(readCurrentField(value: value) === initial)

    let writtenFromSwift = FieldPayload(label: "from-swift")
    value.current = writtenFromSwift
    #expect(readCurrentField(value: value) === writtenFromSwift)

    let writtenFromKotlin = FieldPayload(label: "from-kotlin")
    writeCurrentField(value: value, payload: writtenFromKotlin)
    #expect(value.current === writtenFromKotlin)
    #expect(callSelectedField(value: value) === value.swiftStoredField)
    #expect(callSelectedField(value: value).label == "swift-only")

    let replacement = FieldPayload(label: "replacement")
    let previous = replaceCurrentField(value: value, payload: replacement)
    #expect(previous === writtenFromKotlin)
    #expect(readCurrentField(value: value) === replacement)
}
