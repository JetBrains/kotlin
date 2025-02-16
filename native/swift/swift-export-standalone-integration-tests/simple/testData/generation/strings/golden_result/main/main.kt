@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___getConstantString")
public fun __root___getConstantString(): kotlin.native.internal.NativePtr {
    val _result = getConstantString()
    return _result.objcPtr()
}

@ExportedBridge("__root___getString")
public fun __root___getString(): kotlin.native.internal.NativePtr {
    val _result = getString()
    return _result.objcPtr()
}

@ExportedBridge("__root___setString__TypesOfArguments__Swift_String__")
public fun __root___setString__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): Unit {
    val __value = interpretObjCPointer<kotlin.String>(value)
    setString(__value)
}

@ExportedBridge("__root___string_get")
public fun __root___string_get(): kotlin.native.internal.NativePtr {
    val _result = string
    return _result.objcPtr()
}

@ExportedBridge("__root___string_set__TypesOfArguments__Swift_String__")
public fun __root___string_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    string = __newValue
}

