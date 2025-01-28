@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("namespace_demo_useClassFromA")
public fun namespace_demo_useClassFromA(): kotlin.native.internal.NativePtr {
    val _result = namespace.demo.useClassFromA()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

