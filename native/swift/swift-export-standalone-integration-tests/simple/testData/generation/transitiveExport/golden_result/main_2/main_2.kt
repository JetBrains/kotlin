@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("foo_bar")
public fun foo_bar(): kotlin.native.internal.NativePtr {
    val _result = foo.bar()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("foo_foo")
public fun foo_foo(): kotlin.native.internal.NativePtr {
    val _result = foo.foo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
