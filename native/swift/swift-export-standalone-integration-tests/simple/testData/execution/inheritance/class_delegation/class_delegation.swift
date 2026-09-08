import Inheritance
import Testing

@Test
func kotlinClassDelegationSurvivesSwiftSubclassing() throws {
    class SwiftDelegatingLeaf: DelegatingBase {
        override func localValue() -> String { "swift-local" }
    }

    let value = SwiftDelegatingLeaf()
    let storage = DelegatingStorage()
    storage.store(value: value)
    let retrieved = try #require(storage.retrieve())
    #expect(retrieved === value)
    #expect(callDelegatedValue(value: value) == "delegated-implementation")
    #expect(callDelegatingLocal(value: value) == "swift-local")
}
