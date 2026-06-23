@_implementationOnly import KotlinBridges_collections
import KotlinRuntime
import KotlinRuntimeSupport
import data

public func consume_block_with_dictRef_id(
    block: @escaping ([Swift.String: data.Foo]) -> [Swift.String: data.Foo]
) -> [Swift.String: data.Foo] {
    return __root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___({
        let originalBlock: (Swift.Dictionary<Swift.String,data.Foo>) -> Swift.Dictionary<Swift.String,data.Foo> = block
        return { (arg0: Any) in
            let _arg0: Swift.Dictionary<Swift.String,data.Foo> = arg0 as! Swift.Dictionary<Swift.String,data.Foo>
            let _result = originalBlock(_arg0)
            return _result
        }
    }()) as! Swift.Dictionary<Swift.String,data.Foo>
}
public func consume_block_with_dict_id(
    block: @escaping ([Swift.Int32: Swift.Int32]) -> [Swift.Int32: Swift.Int32]
) -> [Swift.Int32: Swift.Int32] {
    return __root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___({
        let originalBlock: (Swift.Dictionary<Swift.Int32,Swift.Int32>) -> Swift.Dictionary<Swift.Int32,Swift.Int32> = block
        return { (arg0: Any) in
            let _arg0: Swift.Dictionary<Swift.Int32,Swift.Int32> = arg0 as! Swift.Dictionary<Swift.Int32,Swift.Int32>
            let _result = originalBlock(_arg0)
            return Dictionary(uniqueKeysWithValues: _result.map { key, value in (NSNumber(value: key), NSNumber(value: value) )})
        }
    }()) as! Swift.Dictionary<Swift.Int32,Swift.Int32>
}
public func consume_block_with_listRef_id(
    block: @escaping ([data.Foo]) -> [data.Foo]
) -> [data.Foo] {
    return __root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___({
        let originalBlock: (Swift.Array<data.Foo>) -> Swift.Array<data.Foo> = block
        return { (arg0: Any) in
            let _arg0: Swift.Array<data.Foo> = arg0 as! Swift.Array<data.Foo>
            let _result = originalBlock(_arg0)
            return _result
        }
    }()) as! Swift.Array<data.Foo>
}
public func consume_block_with_list_id(
    block: @escaping ([Swift.Int32]) -> [Swift.Int32]
) -> [Swift.Int32] {
    return __root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___({
        let originalBlock: (Swift.Array<Swift.Int32>) -> Swift.Array<Swift.Int32> = block
        return { (arg0: Any) in
            let _arg0: Swift.Array<Swift.Int32> = arg0 as! Swift.Array<Swift.Int32>
            let _result = originalBlock(_arg0)
            return _result.map { it in NSNumber(value: it) }
        }
    }()) as! Swift.Array<Swift.Int32>
}
public func consume_block_with_set_id(
    block: @escaping (Swift.Set<Swift.Int32>) -> Swift.Set<Swift.Int32>
) -> Swift.Set<Swift.Int32> {
    return __root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___({
        let originalBlock: (Swift.Set<Swift.Int32>) -> Swift.Set<Swift.Int32> = block
        return { (arg0: Any) in
            let _arg0: Swift.Set<Swift.Int32> = arg0 as! Swift.Set<Swift.Int32>
            let _result = originalBlock(_arg0)
            return Set(_result.map { it in NSNumber(value: it) })
        }
    }()) as! Swift.Set<Swift.Int32>
}
