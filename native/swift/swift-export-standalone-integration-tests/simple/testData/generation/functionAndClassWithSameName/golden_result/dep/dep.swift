@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dep
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.test.factory.modules {
    public final class ClassFromDependency: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = test_factory_modules_ClassFromDependency_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { test_factory_modules_ClassFromDependency_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
