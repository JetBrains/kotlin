@_implementationOnly import KotlinBridges_primitive_types
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_with_Unit_id(
    block: @escaping (Swift.Void) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_Unit_id__TypesOfArguments__U28Swift_VoidU29202D_U20Swift_Int8__({
        let originalBlock = block
        return { return originalBlock(()) }
    }())
}
public func consume_block_with_Unit_idInTheMix1(
    block: @escaping (Swift.Void, Swift.Int8) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_Unit_idInTheMix1__TypesOfArguments__U28Swift_Void_U20Swift_Int8U29202D_U20Swift_Int8__({
        let originalBlock = block
        return { arg1 in return originalBlock((), arg1) }
    }())
}
public func consume_block_with_Unit_idInTheMix2(
    block: @escaping (Swift.Int8, Swift.Void) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_Unit_idInTheMix2__TypesOfArguments__U28Swift_Int8_U20Swift_VoidU29202D_U20Swift_Int8__({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0, ()) }
    }())
}
public func consume_block_with_Unit_idInTheMix3(
    block: @escaping (Swift.Void, Swift.Void) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_Unit_idInTheMix3__TypesOfArguments__U28Swift_Void_U20Swift_VoidU29202D_U20Swift_Int8__({
        let originalBlock = block
        return { return originalBlock((), ()) }
    }())
}
public func consume_block_with_Unit_idInTheMix4(
    block: @escaping (Swift.Int8, Swift.Void, Swift.Int8) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_Unit_idInTheMix4__TypesOfArguments__U28Swift_Int8_U20Swift_Void_U20Swift_Int8U29202D_U20Swift_Int8__({
        let originalBlock = block
        return { arg0, arg2 in return originalBlock(arg0, (), arg2) }
    }())
}
public func consume_block_with_byte_id(
    block: @escaping (Swift.Int8) -> Swift.Int8
) -> Swift.Int8 {
    return __root___consume_block_with_byte_id__TypesOfArguments__U28Swift_Int8U29202D_U20Swift_Int8__({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0) }
    }())
}
public func consume_block_with_uint_id(
    block: @escaping (Swift.UInt32) -> Swift.UInt32
) -> Swift.UInt32 {
    return __root___consume_block_with_uint_id__TypesOfArguments__U28Swift_UInt32U29202D_U20Swift_UInt32__({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0) }
    }())
}
