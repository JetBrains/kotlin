@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___closure_property_get")
public fun __root___closure_property_get(): kotlin.native.internal.NativePtr {
    val _result = closure_property
    return {
        val newClosure = { kotlin.native.internal.ref.createRetainedExternalRCRef(_result()).toLong() }
        newClosure.objcPtr()
    }()
}

@ExportedBridge("__root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(newValue);
        {
            kotlinFun()
        }
    }()
    closure_property = __newValue
}

@ExportedBridge("__root___foo_1")
public fun __root___foo_1(): kotlin.native.internal.NativePtr {
    val _result = foo_1()
    return {
        val newClosure = { kotlin.native.internal.ref.createRetainedExternalRCRef(_result()).toLong() }
        newClosure.objcPtr()
    }()
}

@ExportedBridge("__root___foo_2")
public fun __root___foo_2(): kotlin.native.internal.NativePtr {
    val _result = foo_2()
    return {
        val newClosure = { kotlin.native.internal.ref.createRetainedExternalRCRef(_result()).toLong() }
        newClosure.objcPtr()
    }()
}

@ExportedBridge("__root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__")
public fun __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__(block: kotlin.native.internal.NativePtr): Unit {
    val __block = {
        val kotlinFun = convertBlockPtrToKotlinFunction<()->Unit>(block);
        {
            kotlinFun()
        }
    }()
    foo_consume_simple(__block)
}
