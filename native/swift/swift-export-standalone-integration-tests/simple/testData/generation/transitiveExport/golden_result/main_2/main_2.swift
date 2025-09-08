@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main_2
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib
import feature

extension ExportedKotlinPackages.foo {
    public static func bar() -> ExportedKotlinPackages.kotlin.Array {
        return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: foo_bar())
    }
    public static func foo() -> ExportedKotlinPackages.oh.my.kotlin.FeatureB {
        return ExportedKotlinPackages.oh.my.kotlin.FeatureB.__createClassWrapper(externalRCRef: foo_foo())
    }
}
