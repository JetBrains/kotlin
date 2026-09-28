@_implementationOnly import KotlinBridges_collections
import KotlinRuntime
import KotlinRuntimeSupport
import data

public func consume_block_with_dictRef_id(
    block: @escaping ([Swift.String: data.Foo]) -> [Swift.String: data.Foo]
) -> [Swift.String: data.Foo] {
    return __root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___(Unmanaged.passRetained((block as (Swift.Dictionary<Swift.String,data.Foo>) -> Swift.Dictionary<Swift.String,data.Foo>) as AnyObject).toOpaque()) as! Swift.Dictionary<Swift.String,data.Foo>
}
public func consume_block_with_dict_id(
    block: @escaping ([Swift.Int32: Swift.Int32]) -> [Swift.Int32: Swift.Int32]
) -> [Swift.Int32: Swift.Int32] {
    return __root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___(Unmanaged.passRetained((block as (Swift.Dictionary<Swift.Int32,Swift.Int32>) -> Swift.Dictionary<Swift.Int32,Swift.Int32>) as AnyObject).toOpaque()) as! Swift.Dictionary<Swift.Int32,Swift.Int32>
}
public func consume_block_with_listRef_id(
    block: @escaping ([data.Foo]) -> [data.Foo]
) -> [data.Foo] {
    return __root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___(Unmanaged.passRetained((block as (Swift.Array<data.Foo>) -> Swift.Array<data.Foo>) as AnyObject).toOpaque()) as! Swift.Array<data.Foo>
}
public func consume_block_with_list_id(
    block: @escaping ([Swift.Int32]) -> [Swift.Int32]
) -> [Swift.Int32] {
    return __root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___(Unmanaged.passRetained((block as (Swift.Array<Swift.Int32>) -> Swift.Array<Swift.Int32>) as AnyObject).toOpaque()) as! Swift.Array<Swift.Int32>
}
public func consume_block_with_set_id(
    block: @escaping (Swift.Set<Swift.Int32>) -> Swift.Set<Swift.Int32>
) -> Swift.Set<Swift.Int32> {
    return __root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___(Unmanaged.passRetained((block as (Swift.Set<Swift.Int32>) -> Swift.Set<Swift.Int32>) as AnyObject).toOpaque()) as! Swift.Set<Swift.Int32>
}
@_cdecl("collections_internal_functional_type_callee_SwiftU2EArrayU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___")
package func collections_internal_functional_type_callee_SwiftU2EArrayU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Any {
    let _result: Swift.Array<Swift.Int32> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Array<Swift.Int32>) -> Swift.Array<Swift.Int32>)(_1 as! Swift.Array<Swift.Int32>)
    return _result.map { it in NSNumber(value: it) }
}

@_cdecl("collections_internal_functional_type_callee_SwiftU2EArrayU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_data_Foo___")
package func collections_internal_functional_type_callee_SwiftU2EArrayU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_data_Foo___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Any {
    let _result: Swift.Array<data.Foo> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Array<data.Foo>) -> Swift.Array<data.Foo>)(_1 as! Swift.Array<data.Foo>)
    return _result
}

@_cdecl("collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EInt32U2CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_Int32_Swift_Int32___")
package func collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EInt32U2CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_Int32_Swift_Int32___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Any {
    let _result: Swift.Dictionary<Swift.Int32,Swift.Int32> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Dictionary<Swift.Int32,Swift.Int32>) -> Swift.Dictionary<Swift.Int32,Swift.Int32>)(_1 as! Swift.Dictionary<Swift.Int32,Swift.Int32>)
    return Dictionary(uniqueKeysWithValues: _result.map { key, value in (NSNumber(value: key), NSNumber(value: value) )})
}

@_cdecl("collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EStringU2CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_String_data_Foo___")
package func collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EStringU2CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_String_data_Foo___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Any {
    let _result: Swift.Dictionary<Swift.String,data.Foo> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Dictionary<Swift.String,data.Foo>) -> Swift.Dictionary<Swift.String,data.Foo>)(_1 as! Swift.Dictionary<Swift.String,data.Foo>)
    return _result
}

@_cdecl("collections_internal_functional_type_callee_SwiftU2ESetU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Set_Swift_Int32___")
package func collections_internal_functional_type_callee_SwiftU2ESetU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Set_Swift_Int32___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Any) -> Any {
    let _result: Swift.Set<Swift.Int32> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Set<Swift.Int32>) -> Swift.Set<Swift.Int32>)(_1 as! Swift.Set<Swift.Int32>)
    return Set(_result.map { it in NSNumber(value: it) })
}
