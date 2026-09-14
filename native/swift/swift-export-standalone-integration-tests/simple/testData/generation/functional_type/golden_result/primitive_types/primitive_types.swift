@_implementationOnly import KotlinBridges_primitive_types
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_with_byte_id(
    block: @escaping (Swift.Int8) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_byte_id__TypesOfArguments__U28Swift_Int8U29202D_U20Swift_Int8__(Unmanaged.passRetained((block as (Swift.Int8) -> Swift.Int8) as AnyObject).toOpaque())
}
public func consume_block_with_uint_id(
    block: @escaping (Swift.UInt32) -> Swift.UInt32
) -> Swift.UInt32 {
    return __root___consume_block_with_uint_id__TypesOfArguments__U28Swift_UInt32U29202D_U20Swift_UInt32__(Unmanaged.passRetained((block as (Swift.UInt32) -> Swift.UInt32) as AnyObject).toOpaque())
}
public func produce_block_with_byte_byte() -> (Swift.Int8, Swift.Int8, Swift.Int8) -> Swift.Int8 {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___produce_block_with_byte_byte(), options: .asBestFittingWrapper)!
        return { _1, _2, _3 in return primitive_types_internal_functional_type_caller_SwiftU2EInt8__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int8_Swift_Int8_Swift_Int8__(pointerToBlock.__externalRCRef()!, _1, _2, _3) }
    }()
}
@_cdecl("primitive_types_internal_functional_type_callee_SwiftU2EInt8__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int8__")
package func primitive_types_internal_functional_type_callee_SwiftU2EInt8__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int8__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int8) -> Swift.Int8 {
    let _result: Swift.Int8 = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int8) -> Swift.Int8)(_1)
    return _result
}

@_cdecl("primitive_types_internal_functional_type_callee_SwiftU2EUInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32__")
package func primitive_types_internal_functional_type_callee_SwiftU2EUInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_UInt32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UInt32) -> Swift.UInt32 {
    let _result: Swift.UInt32 = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.UInt32) -> Swift.UInt32)(_1)
    return _result
}
