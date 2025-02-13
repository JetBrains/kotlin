@_implementationOnly import KotlinBridges_collections
import KotlinRuntimeSupport
import data

public func consume_block_with_dictRef_id(
    block: @escaping @convention(block) (Swift.Dictionary<Swift.String,data.Foo>) -> Swift.Dictionary<Swift.String,data.Foo>
) -> [Swift.String: data.Foo] {
    return __root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0 as! Swift.Dictionary<Swift.String,data.Foo>) }
    }()) as! Swift.Dictionary<Swift.String,data.Foo>
}
public func consume_block_with_dict_id(
    block: @escaping @convention(block) (Swift.Dictionary<Swift.Int32,Swift.Int32>) -> Swift.Dictionary<Swift.Int32,Swift.Int32>
) -> [Swift.Int32: Swift.Int32] {
    return __root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___({
        let originalBlock = block
        return { arg0 in return Dictionary(uniqueKeysWithValues: originalBlock(arg0 as! Swift.Dictionary<Swift.Int32,Swift.Int32>).map { key, value in (NSNumber(value: key), NSNumber(value: value) )}) }
    }()) as! Swift.Dictionary<Swift.Int32,Swift.Int32>
}
public func consume_block_with_listRef_id(
    block: @escaping @convention(block) (Swift.Array<data.Foo>) -> Swift.Array<data.Foo>
) -> [data.Foo] {
    return __root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0 as! Swift.Array<data.Foo>) }
    }()) as! Swift.Array<data.Foo>
}
public func consume_block_with_list_id(
    block: @escaping @convention(block) (Swift.Array<Swift.Int32>) -> Swift.Array<Swift.Int32>
) -> [Swift.Int32] {
    return __root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0 as! Swift.Array<Swift.Int32>).map { it in NSNumber(value: it) } }
    }()) as! Swift.Array<Swift.Int32>
}
public func consume_block_with_set_id(
    block: @escaping @convention(block) (Swift.Set<Swift.Int32>) -> Swift.Set<Swift.Int32>
) -> Swift.Set<Swift.Int32> {
    return __root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___({
        let originalBlock = block
        return { arg0 in return Set(originalBlock(arg0 as! Swift.Set<Swift.Int32>).map { it in NSNumber(value: it) }) }
    }()) as! Swift.Set<Swift.Int32>
}
