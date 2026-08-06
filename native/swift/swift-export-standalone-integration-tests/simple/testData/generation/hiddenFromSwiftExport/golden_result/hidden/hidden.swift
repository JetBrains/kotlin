@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_hidden
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.hidden {
    public protocol HiddenInterface: KotlinRuntime.KotlinBase {
    }
    public final class HiddenClass: KotlinRuntime.KotlinBase {
    }
    open class HiddenOpenClass: KotlinRuntime.KotlinBase {
    }
}
