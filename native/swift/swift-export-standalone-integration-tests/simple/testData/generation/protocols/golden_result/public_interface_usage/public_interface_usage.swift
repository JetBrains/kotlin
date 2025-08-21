@_implementationOnly import KotlinBridges_public_interface_usage
import KotlinRuntime
import KotlinRuntimeSupport
import public_interface

public final class DemoCrossModuleInterfaceUsage: KotlinRuntime.KotlinBase, public_interface.DemoCrossModuleInterface, public_interface._DemoCrossModuleInterface {
    public init() {
        if Self.self != public_interface_usage.DemoCrossModuleInterfaceUsage.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from public_interface_usage.DemoCrossModuleInterfaceUsage ") }
        let __kt = __root___DemoCrossModuleInterfaceUsage_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___DemoCrossModuleInterfaceUsage_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
