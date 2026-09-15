@_implementationOnly import KotlinBridges_simple
import KotlinRuntime
import KotlinRuntimeSupport

public var closure_property: () -> Swift.Void {
    get {
        return {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___closure_property_get(), options: .asBestFittingWrapper)
            return { return { simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()); return () }() }
        }()
    }
    set {
        return { __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__(Unmanaged.passRetained((newValue as () -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
}
public func foo_1() -> () -> Swift.Void {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___foo_1(), options: .asBestFittingWrapper)
        return { return { simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()); return () }() }
    }()
}
public func foo_consume_consuming(
    block: @escaping (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void
) -> Swift.Void {
    return { __root___foo_consume_consuming__TypesOfArguments__U2840escapingU2028Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__(Unmanaged.passRetained((block as (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func foo_consume_consuming_2(
    block: @escaping (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void
) -> Swift.Void {
    return { __root___foo_consume_consuming_2__TypesOfArguments__U2840escapingU2028Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__(Unmanaged.passRetained((block as (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func foo_consume_producing(
    block: @escaping () -> () -> Swift.Void
) -> Swift.Void {
    return { __root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__(Unmanaged.passRetained((block as () -> () -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
public func foo_consume_simple(
    block: @escaping () -> Swift.Void
) -> Swift.Void {
    return { __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__(Unmanaged.passRetained((block as () -> Swift.Void) as AnyObject).toOpaque()); return () }()
}
@_cdecl("simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32___")
package func simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_U28Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void)({
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: _1, options: .asBestFittingWrapper)
    return { _1, _2 in return { let _ref = simple_internal_functional_type_caller_SwiftU2EClosedRangeU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__(pointerToBlock.__externalRCRef(), _1, _2); return kotlin_ranges_intRange_getStart_int_simple(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_simple(_ref) }() }
}())
    return { _result; return true }()
}

@_cdecl("simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func simple_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Void)()
    return { _result; return true }()
}

@_cdecl("simple_internal_functional_type_callee_U2829202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func simple_internal_functional_type_callee_U2829202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: () -> Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> () -> Swift.Void)()
    return Unmanaged.passRetained((_result as () -> Swift.Void) as AnyObject).toOpaque()
}
