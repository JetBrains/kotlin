@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dep
import KotlinRuntime

public extension ExportedKotlinPackages.test.factory.modules {
    public final class ClassFromDependency : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = test_factory_modules_ClassFromDependency_init_allocate()
            super.init(__externalRCRef: __kt)
            test_factory_modules_ClassFromDependency_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
