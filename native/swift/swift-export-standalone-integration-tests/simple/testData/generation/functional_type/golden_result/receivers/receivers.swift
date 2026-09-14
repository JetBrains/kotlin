@_implementationOnly import KotlinBridges_receivers
import KotlinRuntime
import KotlinRuntimeSupport

public func foo(
    i: @escaping (Swift.Int32) -> Swift.Void
) -> Swift.Void {
    return { __root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__(Unmanaged.passRetained((i as (Swift.Int32) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func fooAny(
    i: @escaping (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void
) -> Swift.Void {
    return { __root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__(Unmanaged.passRetained((i as (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func fooList(
    i: @escaping ([Swift.Int32]) -> Swift.Void
) -> Swift.Void {
    return { __root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__(Unmanaged.passRetained((i as (Swift.Array<Swift.Int32>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func fooString(
    i: @escaping (Swift.String?) -> Swift.Void
) -> Swift.Void {
    return { __root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__(Unmanaged.passRetained((i as (Swift.Optional<Swift.String>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
@_cdecl("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___")
package func receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Array<Swift.Int32>) -> Swift.Void)(_1 as! Swift.Array<Swift.Int32>)
    return { _result; return true }()
}

@_cdecl("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
package func receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int32) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int32) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
package func receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.String?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.String>) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__")
package func receivers_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void)(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: _1))
    return { _result; return true }()
}
