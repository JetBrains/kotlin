@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___testListAny__TypesOfArguments__Swift_Array_KotlinRuntime_KotlinBase___")
public fun __root___testListAny__TypesOfArguments__Swift_Array_KotlinRuntime_KotlinBase___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<kotlin.Any>>(l)
    val _result = testListAny(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListInt__TypesOfArguments__Swift_Array_Swift_Int32___")
public fun __root___testListInt__TypesOfArguments__Swift_Array_Swift_Int32___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<Int>>(l)
    val _result = testListInt(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListListInt__TypesOfArguments__Swift_Array_Swift_Array_Swift_Int32____")
public fun __root___testListListInt__TypesOfArguments__Swift_Array_Swift_Array_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<kotlin.collections.List<Int>>>(l)
    val _result = testListListInt(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListNothing__TypesOfArguments__Swift_Array_Swift_Never___")
public fun __root___testListNothing__TypesOfArguments__Swift_Array_Swift_Never___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<Nothing>>(l)
    val _result = testListNothing(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListOptAny__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase____")
public fun __root___testListOptAny__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(l)
    val _result = testListOptAny(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListOptInt__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32____")
public fun __root___testListOptInt__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<Int?>>(l)
    val _result = testListOptInt(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListOptListInt__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Array_Swift_Int32_____")
public fun __root___testListOptListInt__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Array_Swift_Int32_____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<kotlin.collections.List<Int>?>>(l)
    val _result = testListOptListInt(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListOptNothing__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Never____")
public fun __root___testListOptNothing__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Never____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<Nothing?>>(l)
    val _result = testListOptNothing(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListOptString__TypesOfArguments__Swift_Array_Swift_Optional_Swift_String____")
public fun __root___testListOptString__TypesOfArguments__Swift_Array_Swift_Optional_Swift_String____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<kotlin.String?>>(l)
    val _result = testListOptString(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListShort__TypesOfArguments__Swift_Array_Swift_Int16___")
public fun __root___testListShort__TypesOfArguments__Swift_Array_Swift_Int16___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<Short>>(l)
    val _result = testListShort(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testListString__TypesOfArguments__Swift_Array_Swift_String___")
public fun __root___testListString__TypesOfArguments__Swift_Array_Swift_String___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(l)
    val _result = testListString(__l)
    return _result.objcPtr()
}

@ExportedBridge("__root___testOptListInt__TypesOfArguments__Swift_Optional_Swift_Array_Swift_Int32____")
public fun __root___testOptListInt__TypesOfArguments__Swift_Optional_Swift_Array_Swift_Int32____(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = if (l == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.collections.List<Int>>(l)
    val _result = testOptListInt(__l)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}
