@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.org.kotlin {
    public final class Foo: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = org_kotlin_Foo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
