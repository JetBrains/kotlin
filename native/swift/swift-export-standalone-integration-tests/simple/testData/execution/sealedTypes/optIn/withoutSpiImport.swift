// The same module without the SPI import: the opt-in cases are invisible here, so a client that
// never opted in only sees the plain ones and reaches everything else through `default`.
import KotlinRuntime
import SealedOptIn
import Testing

private typealias Root = ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass

private func describeWithoutOptIn(_ value: Root) -> String {
    switch value.sealedType() {
        case let .nonSealedNonOptInClassA(type): "plain: \(type.value)"
        default: "default"
    }
}

@Test
func testWithoutTheSpiImportOptInInheritorsFallIntoDefault() throws {
    let plain = ExportedKotlinPackages.org.kotlin.foo.createNonSealedNonOptInClassA()
    try #require(describeWithoutOptIn(plain) == "plain: NonSealedNonOptInClassA")
    let plainUnderBranch = ExportedKotlinPackages.org.kotlin.foo.createNonSealedNonOptInClassB()
    try #require(describeWithoutOptIn(plainUnderBranch) == "default")
    let optInUnderBranch = ExportedKotlinPackages.org.kotlin.foo.createNonSealedOptInClass()
    try #require(describeWithoutOptIn(optInUnderBranch) == "default")
}
