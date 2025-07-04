@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_Iterator")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_collections_Iterator_hasNext")
public fun kotlin_collections_Iterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = __self.hasNext()
    return _result
}

@ExportedBridge("kotlin_collections_Iterator_next")
public fun kotlin_collections_Iterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = __self.next()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
