@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dep
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.test.factory.modules {
    public final class ClassFromDependency: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.test.factory.modules.ClassFromDependency.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.test.factory.modules.ClassFromDependency ") }
            let __kt = test_factory_modules_ClassFromDependency_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            test_factory_modules_ClassFromDependency_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
