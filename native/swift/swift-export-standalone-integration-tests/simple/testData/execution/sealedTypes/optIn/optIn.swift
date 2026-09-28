// The SPI group name is the annotation's qualified Kotlin name with `$` as the separator.
@_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB) import SealedOptIn
import KotlinRuntime
import Testing

private typealias SealedNonOptInClass = ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass

private func describe(_ value: SealedNonOptInClass) -> String {
    switch value.sealedType() {
        case let .nonSealedNonOptInClassA(type): "plain: \(type.value)"
        case let .sealedOptInClass(.nonSealedNonOptInClassB(type)): "branch-plain: \(type.value)"
        case let .sealedOptInClass(.nonSealedOptInClass(type)): "branch-optIn: \(type.value)"
    }
}

@Test
func testOptInCasesAreReachableThroughAnSpiImport() throws {
    let plainUnderBranch = ExportedKotlinPackages.org.kotlin.foo.createNonSealedNonOptInClassB()
    try #require(describe(plainUnderBranch) == "branch-plain: NonSealedNonOptInClassB")
    let optInUnderBranch = ExportedKotlinPackages.org.kotlin.foo.createNonSealedOptInClass()
    try #require(describe(optInUnderBranch) == "branch-optIn: NonSealedOptInClass")
}

@Test
func testNonOptInSiblingIsUnaffected() throws {
    let plain = ExportedKotlinPackages.org.kotlin.foo.createNonSealedNonOptInClassA()
    try #require(describe(plain) == "plain: NonSealedNonOptInClassA")
}
