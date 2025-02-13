@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyObject::class, "4main8MyObjectC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___MyObject_get")
public fun __root___MyObject_get(): kotlin.native.internal.NativePtr {
    val _result = MyObject
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___getMainObject")
public fun __root___getMainObject(): kotlin.native.internal.NativePtr {
    val _result = getMainObject()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___isMainObject__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun __root___isMainObject__TypesOfArguments__KotlinRuntime_KotlinBase__(obj: kotlin.native.internal.NativePtr): Boolean {
    val __obj = kotlin.native.internal.ref.dereferenceExternalRCRef(obj) as kotlin.Any
    val _result = isMainObject(__obj)
    return _result
}

