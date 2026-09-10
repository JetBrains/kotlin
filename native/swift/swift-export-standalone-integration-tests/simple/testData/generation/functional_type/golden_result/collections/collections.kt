@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("collections_internal_functional_type_callee_SwiftU2EArrayU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___")
internal external fun collections_internal_functional_type_callee_SwiftU2EArrayU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("collections_internal_functional_type_callee_SwiftU2EArrayU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_data_Foo___")
internal external fun collections_internal_functional_type_callee_SwiftU2EArrayU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_data_Foo___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EInt32U2CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_Int32_Swift_Int32___")
internal external fun collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EInt32U2CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_Int32_Swift_Int32___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EStringU2CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_String_data_Foo___")
internal external fun collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EStringU2CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_String_data_Foo___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("collections_internal_functional_type_callee_SwiftU2ESetU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Set_Swift_Int32___")
internal external fun collections_internal_functional_type_callee_SwiftU2ESetU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Set_Swift_Int32___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ExportedBridge("__root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___")
public fun __root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.collections.Map<kotlin.String, Foo> ->
            val _arg0 = arg0.objcPtr()
            val _result = collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EStringU2CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_String_data_Foo___(closureBox.objcPtr(), _arg0)
            interpretObjCPointer<kotlin.collections.Map<kotlin.String, Foo>>(_result)
        }
    }
    val _result = run { consume_block_with_dictRef_id(__block) }
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___")
public fun __root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.collections.Map<Int, Int> ->
            val _arg0 = arg0.objcPtr()
            val _result = collections_internal_functional_type_callee_SwiftU2EDictionaryU3CSwiftU2EInt32U2CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Dictionary_Swift_Int32_Swift_Int32___(closureBox.objcPtr(), _arg0)
            interpretObjCPointer<kotlin.collections.Map<Int, Int>>(_result)
        }
    }
    val _result = run { consume_block_with_dict_id(__block) }
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___")
public fun __root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.collections.List<Foo> ->
            val _arg0 = arg0.objcPtr()
            val _result = collections_internal_functional_type_callee_SwiftU2EArrayU3CdataU2EFooU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_data_Foo___(closureBox.objcPtr(), _arg0)
            interpretObjCPointer<kotlin.collections.List<Foo>>(_result)
        }
    }
    val _result = run { consume_block_with_listRef_id(__block) }
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___")
public fun __root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.collections.List<Int> ->
            val _arg0 = arg0.objcPtr()
            val _result = collections_internal_functional_type_callee_SwiftU2EArrayU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Array_Swift_Int32___(closureBox.objcPtr(), _arg0)
            interpretObjCPointer<kotlin.collections.List<Int>>(_result)
        }
    }
    val _result = run { consume_block_with_list_id(__block) }
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___")
public fun __root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val closurePtr = block;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.collections.Set<Int> ->
            val _arg0 = arg0.objcPtr()
            val _result = collections_internal_functional_type_callee_SwiftU2ESetU3CSwiftU2EInt32U3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Set_Swift_Int32___(closureBox.objcPtr(), _arg0)
            interpretObjCPointer<kotlin.collections.Set<Int>>(_result)
        }
    }
    val _result = run { consume_block_with_set_id(__block) }
    return _result.objcPtr()
}
