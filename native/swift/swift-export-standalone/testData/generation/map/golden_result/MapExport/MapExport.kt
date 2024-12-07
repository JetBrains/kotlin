@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___testMapAnyLong__TypesOfArguments__Swift_Dictionary_KotlinRuntime_KotlinBase_Swift_Int64___")
public fun __root___testMapAnyLong__TypesOfArguments__Swift_Dictionary_KotlinRuntime_KotlinBase_Swift_Int64___(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<kotlin.Any, Long>>(m)
    val _result = testMapAnyLong(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapIntString__TypesOfArguments__Swift_Dictionary_Swift_Int32_Swift_String___")
public fun __root___testMapIntString__TypesOfArguments__Swift_Dictionary_Swift_Int32_Swift_String___(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<Int, kotlin.String>>(m)
    val _result = testMapIntString(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapListIntSetInt__TypesOfArguments__Swift_Dictionary_Swift_Array_Swift_Int32__Swift_Set_Swift_Int32____")
public fun __root___testMapListIntSetInt__TypesOfArguments__Swift_Dictionary_Swift_Array_Swift_Int32__Swift_Set_Swift_Int32____(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<kotlin.collections.List<Int>, kotlin.collections.Set<Int>>>(m)
    val _result = testMapListIntSetInt(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapLongAny__TypesOfArguments__Swift_Dictionary_Swift_Int64_KotlinRuntime_KotlinBase___")
public fun __root___testMapLongAny__TypesOfArguments__Swift_Dictionary_Swift_Int64_KotlinRuntime_KotlinBase___(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<Long, kotlin.Any>>(m)
    val _result = testMapLongAny(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapNothingOptNothing__TypesOfArguments__Swift_Dictionary_Swift_Never_Swift_Optional_Swift_Never____")
public fun __root___testMapNothingOptNothing__TypesOfArguments__Swift_Dictionary_Swift_Never_Swift_Optional_Swift_Never____(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<Nothing, Nothing?>>(m)
    val _result = testMapNothingOptNothing(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapOptIntListInt__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_Int32__Swift_Array_Swift_Int32____")
public fun __root___testMapOptIntListInt__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_Int32__Swift_Array_Swift_Int32____(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<Int?, kotlin.collections.List<Int>>>(m)
    val _result = testMapOptIntListInt(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapOptNothingNothing__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_Never__Swift_Never___")
public fun __root___testMapOptNothingNothing__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_Never__Swift_Never___(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<Nothing?, Nothing>>(m)
    val _result = testMapOptNothingNothing(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapSetIntMapIntInt__TypesOfArguments__Swift_Dictionary_Swift_Set_Swift_Int32__Swift_Dictionary_Swift_Int32_Swift_Int32____")
public fun __root___testMapSetIntMapIntInt__TypesOfArguments__Swift_Dictionary_Swift_Set_Swift_Int32__Swift_Dictionary_Swift_Int32_Swift_Int32____(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<kotlin.collections.Set<Int>, kotlin.collections.Map<Int, Int>>>(m)
    val _result = testMapSetIntMapIntInt(__m)
    return _result.objcPtr()
}

@ExportedBridge("__root___testMapStringInt__TypesOfArguments__Swift_Dictionary_Swift_String_Swift_Int32___")
public fun __root___testMapStringInt__TypesOfArguments__Swift_Dictionary_Swift_String_Swift_Int32___(m: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __m = interpretObjCPointer<kotlin.collections.Map<kotlin.String, Int>>(m)
    val _result = testMapStringInt(__m)
    return _result.objcPtr()
}

