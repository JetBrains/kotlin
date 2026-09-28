@_implementationOnly import KotlinBridges_typealias_to_closure
import KotlinRuntime
import KotlinRuntimeSupport

public typealias CallbackWithInnerClosure = (@escaping () -> Swift.Int32) -> Swift.Int32
public typealias Closure = (Swift.Int32, Swift.Int32) -> Swift.Void
public func foo_flow_with_callback(
    callback: @escaping typealias_to_closure.CallbackWithInnerClosure
) -> typealias_to_closure.CallbackWithInnerClosure {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___foo_flow_with_callback__TypesOfArguments__U2840escapingU202829202D_U20Swift_Int32U29202D_U20Swift_Int32__(Unmanaged.passRetained((callback as (@escaping () -> Swift.Int32) -> Swift.Int32) as AnyObject).toOpaque()), options: .asBestFittingWrapper)!
        return { _1 in return typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__(pointerToBlock.__externalRCRef()!, Unmanaged.passRetained((_1 as () -> Swift.Int32) as AnyObject).toOpaque()) }
    }()
}
public func typealias_demo(
    input: @escaping typealias_to_closure.Closure
) -> typealias_to_closure.Closure {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__(Unmanaged.passRetained((input as (Swift.Int32, Swift.Int32) -> Swift.Void) as AnyObject).toOpaque()), options: .asBestFittingWrapper)!
        return { _1, _2 in return { typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1, _2); return () }() }
    }()
}
@_cdecl("typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__")
package func typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _result: Swift.Int32 = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (@escaping () -> Swift.Int32) -> Swift.Int32)({
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: _1, options: .asBestFittingWrapper)!
    return { return typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!) }
}())
    return _result
}

@_cdecl("typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func typealias_to_closure_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _result: Swift.Int32 = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Int32)()
    return _result
}

@_cdecl("typealias_to_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__")
package func typealias_to_closure_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int32, _ _2: Swift.Int32) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int32, Swift.Int32) -> Swift.Void)(_1, _2)
    return { _result; return true }()
}
