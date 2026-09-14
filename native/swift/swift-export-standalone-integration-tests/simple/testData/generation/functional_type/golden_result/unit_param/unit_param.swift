@_implementationOnly import KotlinBridges_unit_param
import KotlinRuntime
import KotlinRuntimeSupport

public func bar() -> (Swift.String, Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___bar(), options: .asBestFittingWrapper)!
        return { _1, _2 in return { unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(pointerToBlock.__externalRCRef()!, _1, { _2; return true }()); return () }() }
    }()
}
public func barIn(
    block: @escaping (Swift.String, Swift.Void) -> Swift.Void
) -> Swift.Void {
    return { __root___barIn__TypesOfArguments__U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__(Unmanaged.passRetained((block as (Swift.String, Swift.Void) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func baz() -> (@escaping (Swift.String, Swift.Void) -> Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___baz(), options: .asBestFittingWrapper)!
        return { _1 in return { unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_String_U20Swift_VoidU29202D_U20Swift_Void__(pointerToBlock.__externalRCRef()!, Unmanaged.passRetained((_1 as (Swift.String, Swift.Void) -> Swift.Void) as AnyObject).toOpaque()); return () }() }
    }()
}
public func foo() -> (Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___foo(), options: .asBestFittingWrapper)!
        return { _1 in return { unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
    }()
}
public func fooIn(
    block: @escaping (Swift.Void) -> Swift.Void
) -> Swift.Void {
    return { __root___fooIn__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Void__(Unmanaged.passRetained((block as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
@_cdecl("unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__")
package func unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.String, _ _2: Swift.Bool) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.String, Swift.Void) -> Swift.Void)(_1, { _2; return () }())
    return { _result; return true }()
}

@_cdecl("unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
package func unit_param_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Bool) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Void) -> Swift.Void)({ _1; return () }())
    return { _result; return true }()
}
