@_implementationOnly import KotlinBridges_anotherFeature
import KotlinRuntime
import KotlinRuntimeSupport
import state

public final class FeatureC: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var state: ExportedKotlinPackages.oh.my.state.State {
        get {
            return ExportedKotlinPackages.oh.my.state.State(__externalRCRef: FeatureC_state_get(self.__externalRCRef()))
        }
    }
    public override init() {
        let __kt = __root___FeatureC_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___FeatureC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UnsafeMutableRawPointer?
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
