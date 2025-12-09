@_implementationOnly import KotlinBridges_simple
import KotlinRuntime
import KotlinRuntimeSupport

public var closure_property: () -> Swift.Void {
    get {
        return {
            let pointerToBlock = __root___closure_property_get()
            return { return simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock) }
        }()
    }
    set {
        return __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock = newValue
            return { return originalBlock() }
        }())
    }
}
public func foo_1() -> () -> Swift.Void {
    return {
        let pointerToBlock = __root___foo_1()
        return { return simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock) }
    }()
}
public func foo_consume_consuming(
    block: @escaping (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_consuming__TypesOfArguments__U2828Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__({
        let originalBlock = block
        return { arg0 in return originalBlock({
        let pointerToBlock = arg0
        return { _1, _2 in let _result = simple_internal_functional_type_caller_SwiftU2EClosedRangeU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__(pointerToBlock, _1, _2);return kotlin_ranges_intRange_getStart_int_simple(_result) ... kotlin_ranges_intRange_getEndInclusive_int_simple(_result) }
    }()) }
    }())
}
public func foo_consume_consuming_2(
    block: @escaping (@escaping (Swift.UInt32, Swift.UInt32) -> Swift.ClosedRange<Swift.Int32>) -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_consuming_2__TypesOfArguments__U2828Swift_UInt32_U20Swift_UInt32U29202D_U20Swift_ClosedRange_Swift_Int32_U29202D_U20Swift_Void__({
        let originalBlock = block
        return { arg0 in return originalBlock({
        let pointerToBlock = arg0
        return { _1, _2 in let _result = simple_internal_functional_type_caller_SwiftU2EClosedRangeU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32_Swift_UInt32__(pointerToBlock, _1, _2);return kotlin_ranges_intRange_getStart_int_simple(_result) ... kotlin_ranges_intRange_getEndInclusive_int_simple(_result) }
    }()) }
    }())
}
public func foo_consume_producing(
    block: @escaping () -> () -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__({
        let originalBlock = block
        return { return {
        let originalBlock = originalBlock()
        return { return originalBlock() }
    }() }
    }())
}
public func foo_consume_simple(
    block: @escaping () -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__({
        let originalBlock = block
        return { return originalBlock() }
    }())
}
public func foo_sus() -> Swift.Never {
    fatalError()
}
