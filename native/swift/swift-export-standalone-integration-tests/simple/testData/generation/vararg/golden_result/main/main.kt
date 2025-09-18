@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Accessor::class, "4main8AccessorC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Accessor_get__TypesOfArguments__Swift_Int32__")
public fun Accessor_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, i: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor
    val __i = i
    val _result = __self.`get`(__i)
    return _result
}

@ExportedBridge("Accessor_x_get")
public fun Accessor_x_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Accessor
    val _result = __self.x
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
