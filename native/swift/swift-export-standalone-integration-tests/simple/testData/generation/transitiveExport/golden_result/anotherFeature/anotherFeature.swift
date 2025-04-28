@_implementationOnly import KotlinBridges_anotherFeature
import KotlinRuntime
import KotlinRuntimeSupport
import state

public final class FeatureC: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var state: ExportedKotlinPackages.oh.my.state.State {
        get {
            return ExportedKotlinPackages.oh.my.state.State.__create(externalRCRef: FeatureC_state_get(self.__externalRCRef()))
        }
    }
    public init() {
        precondition(Self.self == anotherFeature.FeatureC.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from anotherFeature.FeatureC ")
        let __kt = __root___FeatureC_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___FeatureC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
