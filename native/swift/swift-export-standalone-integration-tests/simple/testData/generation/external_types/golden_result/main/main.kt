@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___consume_nsdate__TypesOfArguments__Foundation_NSDate__")
public fun __root___consume_nsdate__TypesOfArguments__Foundation_NSDate__(date: kotlin.native.internal.NativePtr): Boolean {
    val __date = interpretObjCPointer<platform.Foundation.NSDate>(date)
    val _result = run { consume_nsdate(__date) }
    return run { _result; true }
}

@ExportedBridge("__root___produce_nsdate")
public fun __root___produce_nsdate(): kotlin.native.internal.NativePtr {
    val _result = run { produce_nsdate() }
    return _result.objcPtr()
}

@ExportedBridge("__root___store_nsdate_get")
public fun __root___store_nsdate_get(): kotlin.native.internal.NativePtr {
    val _result = run { store_nsdate }
    return _result.objcPtr()
}

@ExportedBridge("__root___store_nsdate_set__TypesOfArguments__Foundation_NSDate__")
public fun __root___store_nsdate_set__TypesOfArguments__Foundation_NSDate__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<platform.Foundation.NSDate>(newValue)
    val _result = run { store_nsdate = __newValue }
    return run { _result; true }
}
