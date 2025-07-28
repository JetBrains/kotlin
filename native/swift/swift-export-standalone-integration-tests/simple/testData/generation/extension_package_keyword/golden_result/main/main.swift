@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.org.`nonisolated`.kotlin.`internal` {
    public static func foo() -> Swift.Void {
        return org_nonisolated_kotlin_internal_foo()
    }
}
