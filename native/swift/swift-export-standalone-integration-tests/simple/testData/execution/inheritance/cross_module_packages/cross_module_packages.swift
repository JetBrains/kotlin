import Inheritance
import CrossModuleSupport
import Testing

// Packaged Kotlin declarations are relocated under `ExportedKotlinPackages.<package>`; alias them so the test
// body stays readable.
private typealias Middle = ExportedKotlinPackages.middle.pkg.CrossModuleMiddle
private typealias Base = ExportedKotlinPackages.base.pkg.CrossModuleBase

// Overrides one member from each module
private final class SwiftCrossModuleLeaf: Middle {
    override func middleValue() -> Swift.String { "swift-middle" }
    override func baseValue() -> Swift.String { "swift-base" }
}

@Test
func kotlinReachesSwiftOverrideDeclaredInTheMiddleModule() throws {
    let value = SwiftCrossModuleLeaf()
    #expect(ExportedKotlinPackages.middle.pkg.callMiddleValue(value: value) == "swift-middle")
}

@Test
func kotlinReachesSwiftOverrideDeclaredInTheBaseModule() throws {
    let value = SwiftCrossModuleLeaf()
    #expect(ExportedKotlinPackages.base.pkg.callBaseValue(value: value) == "swift-base")
    #expect(ExportedKotlinPackages.middle.pkg.callBaseValueFromMiddle(value: value) == "swift-base")
}
