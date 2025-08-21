@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___")
public fun __root___consume_block_with_dictRef_id__TypesOfArguments__U28Swift_Dictionary_Swift_String_data_Foo_U29202D_U20Swift_Dictionary_Swift_String_data_Foo___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: kotlin.collections.Map<kotlin.String, Foo> ->
            val _result = kotlinFun(arg0.objcPtr())
            interpretObjCPointer<kotlin.collections.Map<kotlin.String, Foo>>(_result)
        }
    }
    val _result = consume_block_with_dictRef_id(__block)
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___")
public fun __root___consume_block_with_dict_id__TypesOfArguments__U28Swift_Dictionary_Swift_Int32_Swift_Int32_U29202D_U20Swift_Dictionary_Swift_Int32_Swift_Int32___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: kotlin.collections.Map<Int, Int> ->
            val _result = kotlinFun(arg0.objcPtr())
            interpretObjCPointer<kotlin.collections.Map<Int, Int>>(_result)
        }
    }
    val _result = consume_block_with_dict_id(__block)
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___")
public fun __root___consume_block_with_listRef_id__TypesOfArguments__U28Swift_Array_data_Foo_U29202D_U20Swift_Array_data_Foo___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: kotlin.collections.List<Foo> ->
            val _result = kotlinFun(arg0.objcPtr())
            interpretObjCPointer<kotlin.collections.List<Foo>>(_result)
        }
    }
    val _result = consume_block_with_listRef_id(__block)
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___")
public fun __root___consume_block_with_list_id__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Array_Swift_Int32___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: kotlin.collections.List<Int> ->
            val _result = kotlinFun(arg0.objcPtr())
            interpretObjCPointer<kotlin.collections.List<Int>>(_result)
        }
    }
    val _result = consume_block_with_list_id(__block)
    return _result.objcPtr()
}

@ExportedBridge("__root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___")
public fun __root___consume_block_with_set_id__TypesOfArguments__U28Swift_Set_Swift_Int32_U29202D_U20Swift_Set_Swift_Int32___(block: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __block = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(block);
        { arg0: kotlin.collections.Set<Int> ->
            val _result = kotlinFun(arg0.objcPtr())
            interpretObjCPointer<kotlin.collections.Set<Int>>(_result)
        }
    }
    val _result = consume_block_with_set_id(__block)
    return _result.objcPtr()
}
