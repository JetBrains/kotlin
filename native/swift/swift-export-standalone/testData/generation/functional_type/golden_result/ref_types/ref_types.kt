@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___produce_block_with_reftype")
public fun __root___produce_block_with_reftype(): kotlin.native.internal.NativePtr {
    val _result = produce_block_with_reftype()
    return {
        val newClosure: (Long, Long) -> Long = { arg0, arg1 ->
            val res = _result(arg0.toCPointer<CPointed>()!!.asStableRef<Foo>().get(), arg1.toCPointer<CPointed>()!!.asStableRef<Bar>().get())
            kotlin.native.internal.ref.createRetainedExternalRCRef(res).toLong()
        }
        newClosure.objcPtr()
    }()
}

