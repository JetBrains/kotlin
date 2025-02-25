@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dependency_deeper_neighbor_exported
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.dependency.four {
    public final class AnotherBar: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = dependency_four_AnotherBar_init_allocate()
            super.init(__externalRCRef: __kt)
            dependency_four_AnotherBar_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
