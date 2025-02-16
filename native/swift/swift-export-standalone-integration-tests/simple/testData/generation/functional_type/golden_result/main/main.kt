@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___closure_property_get")
public fun __root___closure_property_get(): kotlin.native.internal.NativePtr {
    val _result = closure_property
    return {
        val newClosure: () -> Long = {
            val res = _result()
            kotlin.native.internal.ref.createRetainedExternalRCRef(res).toLong()
        }
        newClosure.objcPtr()
    }()
}

@ExportedBridge("__root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = interpretObjCPointer<Function0<kotlin.Unit>>(newValue)
    closure_property = __newValue
}

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

@ExportedBridge("__root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = interpretObjCPointer<Function0<kotlin.Unit>>(block)
    foo_consume_simple(__block)
}

