@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___testOptSetInt__TypesOfArguments__Swift_Set_Swift_Int32__opt___")
public fun __root___testOptSetInt__TypesOfArguments__Swift_Set_Swift_Int32__opt___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = if (s == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.collections.Set<Int>>(s)
    val _result = testOptSetInt(__s)
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("__root___testSetAny__TypesOfArguments__Swift_Set_KotlinRuntime_KotlinBase___")
public fun __root___testSetAny__TypesOfArguments__Swift_Set_KotlinRuntime_KotlinBase___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.Any>>(s)
    val _result = testSetAny(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetInt__TypesOfArguments__Swift_Set_Swift_Int32___")
public fun __root___testSetInt__TypesOfArguments__Swift_Set_Swift_Int32___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<Int>>(s)
    val _result = testSetInt(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetListInt__TypesOfArguments__Swift_Set_Swift_Array_Swift_Int32____")
public fun __root___testSetListInt__TypesOfArguments__Swift_Set_Swift_Array_Swift_Int32____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.collections.List<Int>>>(s)
    val _result = testSetListInt(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetNothing__TypesOfArguments__Swift_Set_Swift_Never___")
public fun __root___testSetNothing__TypesOfArguments__Swift_Set_Swift_Never___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<Nothing>>(s)
    val _result = testSetNothing(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetOptAny__TypesOfArguments__Swift_Set_Swift_Optional_KotlinRuntime_KotlinBase____")
public fun __root___testSetOptAny__TypesOfArguments__Swift_Set_Swift_Optional_KotlinRuntime_KotlinBase____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.Any?>>(s)
    val _result = testSetOptAny(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetOptInt__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Int32____")
public fun __root___testSetOptInt__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Int32____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<Int?>>(s)
    val _result = testSetOptInt(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetOptNothing__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Never____")
public fun __root___testSetOptNothing__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Never____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<Nothing?>>(s)
    val _result = testSetOptNothing(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetOptSetInt__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Set_Swift_Int32_____")
public fun __root___testSetOptSetInt__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Set_Swift_Int32_____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.collections.Set<Int>?>>(s)
    val _result = testSetOptSetInt(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetOptString__TypesOfArguments__Swift_Set_Swift_Optional_Swift_String____")
public fun __root___testSetOptString__TypesOfArguments__Swift_Set_Swift_Optional_Swift_String____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.String?>>(s)
    val _result = testSetOptString(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetSetInt__TypesOfArguments__Swift_Set_Swift_Set_Swift_Int32____")
public fun __root___testSetSetInt__TypesOfArguments__Swift_Set_Swift_Set_Swift_Int32____(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.collections.Set<Int>>>(s)
    val _result = testSetSetInt(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetShort__TypesOfArguments__Swift_Set_Swift_Int16___")
public fun __root___testSetShort__TypesOfArguments__Swift_Set_Swift_Int16___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<Short>>(s)
    val _result = testSetShort(__s)
    return _result.objcPtr()
}

@ExportedBridge("__root___testSetString__TypesOfArguments__Swift_Set_Swift_String___")
public fun __root___testSetString__TypesOfArguments__Swift_Set_Swift_String___(s: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __s = interpretObjCPointer<kotlin.collections.Set<kotlin.String>>(s)
    val _result = testSetString(__s)
    return _result.objcPtr()
}

