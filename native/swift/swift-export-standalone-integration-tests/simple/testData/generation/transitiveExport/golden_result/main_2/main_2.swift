@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main_2
import KotlinRuntime
import KotlinRuntimeSupport
import feature

public extension ExportedKotlinPackages.foo {
    public static func foo() -> ExportedKotlinPackages.oh.my.kotlin.FeatureB {
        return ExportedKotlinPackages.oh.my.kotlin.FeatureB.__create(externalRCRef: foo_foo())
    }
}
