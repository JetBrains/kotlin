@_implementationOnly import KotlinBridges_public_interface_usage
import KotlinRuntime
import KotlinRuntimeSupport
import public_interface

public final class DemoCrossModuleInterfaceUsage: KotlinRuntime.KotlinBase, public_interface.DemoCrossModuleInterface, public_interface._DemoCrossModuleInterface {
    public init() {
        let __kt = __root___DemoCrossModuleInterfaceUsage_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___DemoCrossModuleInterfaceUsage_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
