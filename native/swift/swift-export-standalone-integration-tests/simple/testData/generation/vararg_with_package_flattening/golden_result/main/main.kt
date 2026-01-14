@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("org_kotlin_foo_log__TypesOfArguments__Swift_Array_Swift_String___")
public fun org_kotlin_foo_log__TypesOfArguments__Swift_Array_Swift_String___(messages: kotlin.native.internal.NativePtr): Unit {
    val __messages = interpretObjCPointer<kotlin.collections.List<kotlin.String>>(messages).toTypedArray()
    org.kotlin.foo.log(*__messages)
}
