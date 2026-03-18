@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main_two
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.org.main.second {
    public static var deps_instance_2: any KotlinRuntimeSupport._KotlinBridgeable {
        get {
            return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: org_main_second_deps_instance_2_get())
        }
    }
}
