@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.hidden {
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func describe(
        _ receiver: Swift.Never
    ) -> Swift.String {
        fatalError()
    }
}
extension ExportedKotlinPackages.koin.like {
    public final class Module: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = koin_like_Module_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { koin_like_Module_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func module(
        _ receiver: Swift.Never
    ) -> ExportedKotlinPackages.koin.like.Module {
        fatalError()
    }
}
