import Inheritance
import Testing

// Serialized and ordered from the least to the most suspicious case, so that a crash identifies the first
// failing scenario.
@Suite(.serialized)
struct CovariantInterfaceOverride {
    class SwiftCat: Cat {
        override func name() -> String { "swift-cat" }
    }

    @Test
    func kotlinInstance() throws {
        #expect(adoptViaInterface(s: CatShelter()) == "kotlin-cat")
        #expect(adoptViaClass(s: CatShelter()) == "kotlin-cat")
        #expect(titleViaInterface(n: SureNamer()) == "kotlin-title")
    }

    @Test
    func swiftOverrideViaClass() throws {
        class SwiftShelter: CatShelter {
            override func adopt() -> Cat { SwiftCat() }
        }
        #expect(adoptViaClass(s: SwiftShelter()) == "swift-cat")
    }

    @Test
    func swiftOverrideViaInterface() throws {
        class SwiftShelter: CatShelter {
            override func adopt() -> Cat { SwiftCat() }
        }
        #expect(adoptViaInterface(s: SwiftShelter()) == "swift-cat")
        #expect((SwiftShelter() as any Shelter).adopt().name() == "swift-cat")
    }

    @Test
    func swiftSubclassWithoutOverrideViaInterface() throws {
        class PlainShelter: CatShelter {}
        #expect(adoptViaInterface(s: PlainShelter()) == "kotlin-cat")
    }

    @Test
    func swiftNullabilityOverrideViaInterface() throws {
        class SwiftNamer: SureNamer {
            override func title() -> String { "swift-title" }
        }
        #expect(titleViaClass(n: SwiftNamer()) == "swift-title")
        #expect(titleViaInterface(n: SwiftNamer()) == "swift-title")
    }
}
