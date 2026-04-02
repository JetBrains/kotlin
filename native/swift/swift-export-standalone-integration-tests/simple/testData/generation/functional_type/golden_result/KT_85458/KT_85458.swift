@_implementationOnly import KotlinBridges_KT_85458
import KotlinRuntime
import KotlinRuntimeSupport

public typealias OnCancellationConstructor = () -> () -> Swift.Void
public var onCancellationConstructor: KT_85458.OnCancellationConstructor? {
    get {
        return __root___onCancellationConstructor_get().map { it in {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)!
            return { return {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: KT_85458_internal_functional_type_caller_U2829202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!), options: .asBestFittingWrapper)!
            return { return { KT_85458_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!); return () }() }
        }() }
        }() }
    }
}
