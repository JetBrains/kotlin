@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_stdlib
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.kotlin {
    open class Enum: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
}
