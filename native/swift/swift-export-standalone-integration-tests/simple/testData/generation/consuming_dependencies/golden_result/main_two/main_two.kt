@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("org_main_second_deps_instance_2_get")
public fun org_main_second_deps_instance_2_get(): kotlin.native.internal.NativePtr {
    val _result = org.main.second.deps_instance_2
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

