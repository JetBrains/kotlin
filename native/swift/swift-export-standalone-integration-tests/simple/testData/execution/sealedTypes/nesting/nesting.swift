import KotlinRuntime
import SealedNesting
import Testing

// Nested scopes are flattened into one generated declaration, but the nested path a client writes
// stays available through typealiases.
private typealias NestedRoot = Outer.Middle.Inner.Root
private typealias SplitRoot = Holder.Nested.SplitRoot

private func describeNested(_ value: any NestedRoot) -> String {
    switch value.sealedType() {
        case let .leaf(type): "leaf: \(type.value)"
        case let .leafObject(type): "leafObject: \(type.value)"
        case let .unknown(type): "unknown: \(type.value)"
    }
}

@Test
func testInheritorsNestedInTheRootsOwnContainer() throws {
    try #require(describeNested(createNestedLeaf()) == "leaf: Leaf")
    try #require(describeNested(createNestedLeafObject()) == "leafObject: LeafObject")
    // The leaves are also constructible through that same nested path.
    try #require(describeNested(Outer.Middle.Inner.Leaf()) == "leaf: Leaf")
    try #require(describeNested(Outer.Middle.Inner.LeafObject.shared) == "leafObject: LeafObject")
}

@Test
func testNonExportedNestedInheritorReportsUnknown() throws {
    try #require(describeNested(createNestedHidden()) == "unknown: Hidden")
}

private func describeSplit(_ value: any SplitRoot) -> String {
    switch value.sealedType() {
        case let .splitTopLevel(type): "topLevel: \(type.value)"
        case let .splitDeeper(type): "deeper: \(type.value)"
        case let .unknown(type): "unknown: \(type.value)"
    }
}

@Test
func testTopLevelInheritorOfANestedRoot() throws {
    try #require(describeSplit(createSplitTopLevel()) == "topLevel: SplitTopLevel")
}

@Test
func testInheritorNestedOutsideTheRootsContainer() throws {
    try #require(describeSplit(createSplitDeeper()) == "deeper: SplitDeeper")
    try #require(describeSplit(OtherHolder.Deeper.SplitDeeper()) == "deeper: SplitDeeper")
}

@Test
func testNonExportedTopLevelInheritorReportsUnknown() throws {
    try #require(describeSplit(createSplitHidden()) == "unknown: SplitHidden")
}
