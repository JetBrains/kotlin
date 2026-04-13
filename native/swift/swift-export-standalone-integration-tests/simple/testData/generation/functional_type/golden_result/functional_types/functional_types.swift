@_implementationOnly import KotlinBridges_functional_types
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_consuming_block(
    block: @escaping (@escaping () -> Swift.Void) -> Swift.Void
) -> Swift.Void {
    return { __root___consume_block_consuming_block__TypesOfArguments__U2840escapingU202829202D_U20Swift_VoidU29202D_U20Swift_Void__({
        let originalBlock = block
        return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock({
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: arg0, options: .asBestFittingWrapper)!
        return { return { functional_types_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!); return () }() }
    }()); return true }() }
    }()); return () }()
}
