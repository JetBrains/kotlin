import KotlinRuntime
import SealedNaming
import Testing

private typealias MySealedClass = ExportedKotlinPackages.org.kotlin.foo.MySealedClass

private func describe(_ value: MySealedClass) -> String {
    switch value.sealedType() {
        case let .myClassAInner(.leaf(type)): "a: \(type.value)"
        case let .myClassBInner(.leaf(type)): "b: \(type.value)"
    }
}

@Test
func testInheritorsWithTheSameSimpleNameGetDistinctCases() throws {
    try #require(describe(ExportedKotlinPackages.org.kotlin.foo.createMyClassALeaf()) == "a: MyClassA.Inner.Leaf")
    try #require(describe(ExportedKotlinPackages.org.kotlin.foo.createMyClassBLeaf()) == "b: MyClassB.Inner.Leaf")
}

@Test
func testInheritorsThatDifferOnlyInSeparatorsGetDistinctCases() throws {
    let underscored = ExportedKotlinPackages.org.kotlin.foo.createFooUnderscoreBar().sealedType()
    let plain = ExportedKotlinPackages.org.kotlin.foo.createFooBar().sealedType()
    try #require("\(underscored.value)" == "Foo_Bar")
    try #require("\(plain.value)" == "FooBar")
    try #require("\(underscored)" != "\(plain)")
}
