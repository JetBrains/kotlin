import KotlinRuntime
import SealedDeprecation
import Testing

private typealias SealedClassNonDeprecated = ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated

// Naming the deprecated declarations warns on purpose. The `@available` attributes themselves have
// no runtime effect, so what this case covers is which cases exist, not how they are annotated.

private func describe(_ value: SealedClassNonDeprecated) -> String {
    switch value.sealedType() {
        case let .deprecatedWarningSubClass(type): "warning: \(type.value)"
        // `DeprecatedErrorSubClass` is `@available(*, unavailable)`, so it has no case at all.
        case let .unknown(type): "unknown: \(type.value)"
    }
}

@Test
func testWarningDeprecatedInheritorKeepsItsOwnCase() throws {
    let value = ExportedKotlinPackages.org.kotlin.foo.createDeprecatedWarningSubClass()
    try #require(describe(value) == "warning: DeprecatedWarningSubClass")
}

@Test
func testErrorDeprecatedInheritorFallsBackToUnknown() throws {
    let value = ExportedKotlinPackages.org.kotlin.foo.createDeprecatedErrorSubClass()
    try #require(describe(value) == "unknown: DeprecatedErrorSubClass")
}

@Test
func testDeprecatedRootStillReportsItsInheritor() throws {
    let value = ExportedKotlinPackages.org.kotlin.foo.createNonDeprecatedSubClassB()
    let result = switch value.sealedType() {
        case let .nonDeprecatedSubClassB(type): "inheritor: \(type.value)"
    }
    try #require(result == "inheritor: NonDeprecatedSubClassB")
}
