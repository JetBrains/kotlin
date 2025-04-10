@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ByteArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ByteArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.ByteIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE12ByteIteratorC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__")
public fun kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self: kotlin.native.internal.NativePtr, index: Int, value: Byte): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_ByteArray_size_get")
public fun kotlin_ByteArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val _result = __self.size
    return _result
}
