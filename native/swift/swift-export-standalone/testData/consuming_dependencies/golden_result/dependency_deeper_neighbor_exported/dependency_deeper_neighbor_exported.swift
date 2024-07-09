@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dependency_deeper_neighbor_exported
import KotlinRuntime

public extension ExportedKotlinPackages.dependency.four {
    public class AnotherBar : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = dependency_four_AnotherBar_init_allocate()
            super.init(__externalRCRef: __kt)
            dependency_four_AnotherBar_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
