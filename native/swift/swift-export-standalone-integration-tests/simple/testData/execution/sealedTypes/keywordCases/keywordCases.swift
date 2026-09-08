import KotlinRuntime
import SealedKeywordCases
import Testing

// Packaged Kotlin declarations are relocated under `ExportedKotlinPackages.<package>`.
private typealias KeywordHost = ExportedKotlinPackages.keyword.host.KeywordHost

private func describe(_ host: any KeywordHost) -> String {
    switch host.sealedType() {
        case let .`case`(type): "case: \(type.value)"
        case let .`default`(type): "default: \(type.value)"
        case let .`init`(type): "init: \(type.value)"
        case let .`self`(type): "self: \(type.value)"
    }
}

@Test
func testKeywordNamedCasesAreMatchable() throws {
    try #require(describe(ExportedKotlinPackages.keyword.host.createDefault()) == "default: Default")
    try #require(describe(ExportedKotlinPackages.keyword.host.createCase()) == "case: Case")
    try #require(describe(ExportedKotlinPackages.keyword.host.createInit()) == "init: Init")
    try #require(describe(ExportedKotlinPackages.keyword.host.createSelf()) == "self: Self")
}
