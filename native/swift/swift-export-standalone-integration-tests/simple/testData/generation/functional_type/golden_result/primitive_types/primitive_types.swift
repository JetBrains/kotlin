@_implementationOnly import KotlinBridges_primitive_types
import KotlinRuntimeSupport

public func consume_block_with_byte_id(
    block: @escaping @convention(block) (Swift.Int8) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_byte_id__TypesOfArguments__U28Swift_Int8U29202D_U20Swift_Int8__({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0) }
    }())
}
public func consume_block_with_uint_id(
    block: @escaping @convention(block) (Swift.UInt32) -> Swift.UInt32
) -> Swift.UInt32 {
    return __root___consume_block_with_uint_id__TypesOfArguments__U28Swift_UInt32U29202D_U20Swift_UInt32__({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0) }
    }())
}
