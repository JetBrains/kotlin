@_implementationOnly import KotlinBridges_inline
import KotlinRuntime
import KotlinRuntimeSupport

public func bar(
    inlined: @escaping () -> Swift.Void,
    notInlined: @escaping () -> Swift.Void
) -> Swift.Void {
    return { __root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__(Unmanaged.passRetained((inlined as () -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((notInlined as () -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func foo(
    inlined: @escaping () -> Swift.Void
) -> Swift.Void {
    return { __root___foo__TypesOfArguments__U2829202D_U20Swift_Void__(Unmanaged.passRetained((inlined as () -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
@_cdecl("inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func inline_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Void)()
    return { _result; return true }()
}
