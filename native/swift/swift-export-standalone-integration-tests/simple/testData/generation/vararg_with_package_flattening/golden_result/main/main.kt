@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("org_kotlin_foo_log__TypesOfArguments__Swift_Array_Swift_String__Vararg___")
public fun org_kotlin_foo_log__TypesOfArguments__Swift_Array_Swift_String__Vararg___(messages: kotlin.native.internal.NativePtr): Boolean {
    val __messages = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(messages).toTypedArray()
    val _result = run { org.kotlin.foo.log(*__messages) }
    return run { _result; true }
}
