@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___foo_1")
public fun __root___foo_1(): kotlin.native.internal.NativePtr {
    val _result = foo_1()
    return {
       val newClosure: () -> Long = {
           val res = _result()
           kotlin.native.internal.ref.createRetainedExternalRCRef(res).toLong()
       }
       newClosure.objcPtr()
    }()
}

@ExportedBridge("__root___foo_2")
public fun __root___foo_2(): kotlin.native.internal.NativePtr {
    val _result = foo_2()
    return {
       val newClosure: () -> Long = {
           val res = _result()
           kotlin.native.internal.ref.createRetainedExternalRCRef(res).toLong()
       }
       newClosure.objcPtr()
    }()
}

