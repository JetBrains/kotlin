@_implementationOnly import KotlinBridges_functional_types
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_consuming_block(
    block: @escaping (@escaping () -> Swift.Void) -> Swift.Void
) -> Swift.Void {
    return { __root___consume_block_consuming_block__TypesOfArguments__U2840escapingU202829202D_U20Swift_VoidU29202D_U20Swift_Void__(Unmanaged.passRetained((block as (@escaping () -> Swift.Void) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
@_cdecl("functional_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Void__")
package func functional_types_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Void__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (@escaping () -> Swift.Void) -> Swift.Void)({
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: _1, options: .asBestFittingWrapper)!
    return { return { functional_types_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!); return () }() }
}())
    return { _result; return true }()
}
