/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class, ExperimentalAtomicApi::class)

@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // Char.toInt()
package kotlin.native.internal

import kotlin.experimental.ExperimentalNativeApi
import kotlin.internal.getProgressionLastElement
import kotlin.reflect.KClass
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.cinterop.*
import kotlinx.cinterop.NativePtr
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.ref.ExternalRCRef
import kotlin.native.internal.ref.dereferenceExternalRCRef
import kotlin.native.internal.ref.disposeExternalRCRef
import kotlin.native.internal.ref.releaseExternalRCRef

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowIndexOutOfBoundsException(): Nothing {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    @Suppress("DEPRECATION")
    throw ArrayIndexOutOfBoundsException()
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowClassCastException(instance: Any, typeInfo: NativePtr): Nothing {
    val clazz = KClassImpl<Any>(typeInfo)
    throw ClassCastException("${instance::class} cannot be cast to $clazz")
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowTypeCastException(instance: Any, typeName: String): Nothing {
    throw TypeCastException("${instance::class} cannot be cast to class $typeName")
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowKotlinNothingValueException(): Nothing {
    throw KotlinNothingValueException()
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowInvalidReceiverTypeException(klass: KClass<*>): Nothing {
    throw RuntimeException("Unexpected receiver type: " + (klass.qualifiedName ?: "noname"))
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowArithmeticException() : Nothing {
    throw ArithmeticException()
}

@ExportForCppRuntime
internal fun ThrowNumberFormatException() : Nothing {
    throw NumberFormatException()
}

@ExportForCppRuntime
internal fun ThrowOutOfMemoryError() : Nothing {
    throw OutOfMemoryError()
}

@UsedFromCompilerGeneratedCode
internal fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowIllegalArgumentException() : Nothing {
    throw IllegalArgumentException()
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowIllegalArgumentExceptionWithMessage(message: String) : Nothing {
    throw IllegalArgumentException(message)
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowIllegalStateException() : Nothing {
    throw IllegalStateException()
}

@ExportForCppRuntime
@UsedFromCompilerGeneratedCode
internal fun ThrowIllegalStateExceptionWithMessage(message:String) : Nothing {
    throw IllegalStateException(message)
}


@ExportForCppRuntime
internal fun ThrowNotImplementedError(): Nothing {
    throw NotImplementedError("An operation is not implemented.")
}

@ExportForCppRuntime
internal fun ThrowCharacterCodingException(): Nothing {
    throw CharacterCodingException()
}

internal class FileFailedToInitializeException(message: String?, cause: Throwable?) : Error(message, cause)

@ExportForCppRuntime
@OptIn(ExperimentalStdlibApi::class)
internal fun ThrowFileFailedToInitializeException(reason: Throwable?) {
    if (reason is Error) {
        throw reason
    } else {
        // https://youtrack.jetbrains.com/issue/KT-57134
        // TODO: align exact exception hierarchy with jvm
        // in jvm it's NoClassDefFound if reason is null, i.e. this is already failed class
        // and ExceptionInInitializerError if it's non-null
        throw FileFailedToInitializeException("There was an error during file or class initialization", reason)
    }
}

@ExportForCppRuntime
internal fun ThrowRuntimeException(message: String?): Nothing {
    throw RuntimeException(message)
}

@ExportForCppRuntime
internal fun PrintThrowable(throwable: Throwable) {
    println(throwable)
}

@ExportForCppRuntime
internal fun ReportUnhandledException(throwable: Throwable) {
    print("Uncaught Kotlin exception: ")
    throwable.printStackTrace()
}

// Using object to make sure that `hook` is initialized when it's needed instead of
// in a normal global initialization flow. This is important if some global happens
// to throw an exception during it's initialization before this hook would've been initialized.
@OptIn(ExperimentalNativeApi::class)
internal object UnhandledExceptionHookHolder {
    internal val hook: AtomicReference<ReportUnhandledExceptionHook?> = AtomicReference<ReportUnhandledExceptionHook?>(null)
}

@ExportForCppRuntime("Kotlin_runUnhandledExceptionHook")
@OptIn(ExperimentalNativeApi::class)
internal fun runUnhandledExceptionHook(throwable: Throwable) {
    val handler = UnhandledExceptionHookHolder.hook.load() ?: throw throwable
    handler(throwable)
}

@ExportForCppRuntime
internal fun TheEmptyString() = ""

@UsedFromCompilerGeneratedCode
internal fun <T: Enum<T>> valueOfForEnum(name: String, values: Array<T>) : T {
    var left = 0
    var right = values.size - 1
    while (left <= right) {
        val middle = (left + right) / 2
        val x = values[middle].name.compareTo(name)
        when {
            x < 0 -> left = middle + 1
            x > 0 -> right = middle - 1
            else -> return values[middle]
        }
    }
    throw IllegalArgumentException("Invalid enum value name: $name")
}

@UsedFromCompilerGeneratedCode
internal fun <T: Enum<T>> valuesForEnum(values: Array<T>): Array<T> {
    val result = @Suppress("TYPE_PARAMETER_AS_REIFIED") Array<T?>(values.size)
    for (value in values)
        result[value.ordinal] = value
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

@TypedIntrinsic(IntrinsicType.CREATE_UNINITIALIZED_INSTANCE)
@InternalForKotlinNative
public external fun <T> createUninitializedInstance(): T

@TypedIntrinsic(IntrinsicType.INIT_INSTANCE)
@InternalForKotlinNative
public external fun initInstance(thiz: Any, constructorCall: Any): Unit

@TypedIntrinsic(IntrinsicType.CREATE_UNINITIALIZED_ARRAY)
@InternalForKotlinNative
public external fun <T> createUninitializedArray(size: Int): T

@UsedFromCompilerGeneratedCode
@TypedIntrinsic(IntrinsicType.CREATE_EMPTY_STRING)
@InternalForKotlinNative
internal external fun createEmptyString(): String

@UsedFromCompilerGeneratedCode
@TypedIntrinsic(IntrinsicType.IS_SUBTYPE)
internal external fun <T> isSubtype(objTypeInfo: NativePtr): Boolean

// Called by the debugger.
@ExportForCppRuntime
internal fun KonanObjectToUtf8Array(value: Any?): ByteArray {
    val string = try {
        when (value) {
            is List<*> -> collectionDebugString("List", value.size, value)
            is Set<*> -> collectionDebugString("Set", value.size, value)
            is Map<*, *> -> collectionDebugString("Map", value.size, value.entries)
            is Array<*> -> arrayDebugString("Array", value.size) { value[it] }
            is CharArray -> arrayDebugString("CharArray", value.size) { value[it] }
            is BooleanArray -> arrayDebugString("BooleanArray", value.size) { value[it] }
            is ByteArray -> arrayDebugString("ByteArray", value.size) { value[it] }
            is ShortArray -> arrayDebugString("ShortArray", value.size) { value[it] }
            is IntArray -> arrayDebugString("IntArray", value.size) { value[it] }
            is LongArray -> arrayDebugString("LongArray", value.size) { value[it] }
            is FloatArray -> arrayDebugString("FloatArray", value.size) { value[it] }
            is DoubleArray -> arrayDebugString("DoubleArray", value.size) { value[it] }
            else -> value.toString()
        }
    } catch (error: Throwable) {
        "<Thrown $error when converting to string>"
    }
    return string.encodeToByteArray()
}

private fun collectionDebugString(
    type: String,
    size: Int,
    elements: Iterable<*>,
): String = buildString {
    append(type)
    append("(size=")
    append(size)
    append(") [")

    val iterator = elements.iterator()
    val displayedElements = if (size > 10) 9 else size
    repeat(displayedElements) { index ->
        if (index > 0) append(", ")
        append(collectionDebugElementString(iterator.next()))
    }
    if (size > 10) {
        var lastElement = iterator.next()
        while (iterator.hasNext()) {
            lastElement = iterator.next()
        }
        append(", ..., ")
        append(collectionDebugElementString(lastElement))
    }
    append(']')
}

private inline fun arrayDebugString(
    type: String,
    size: Int,
    elementAt: (Int) -> Any?,
): String = buildString {
    append(type)
    append("(size=")
    append(size)
    append(") [")

    val displayedElements = if (size > 10) 9 else size
    repeat(displayedElements) { index ->
        if (index > 0) append(", ")
        append(collectionDebugElementString(elementAt(index)))
    }
    if (size > 10) {
        append(", ..., ")
        append(collectionDebugElementString(elementAt(size - 1)))
    }
    append(']')
}

private fun collectionDebugElementString(value: Any?): String = when (value) {
    is List<*> -> "List(size=${value.size})"
    is Set<*> -> "Set(size=${value.size})"
    is Map<*, *> -> "Map(size=${value.size})"
    is Array<*> -> "Array(size=${value.size})"
    is CharArray -> "CharArray(size=${value.size})"
    is BooleanArray -> "BooleanArray(size=${value.size})"
    is ByteArray -> "ByteArray(size=${value.size})"
    is ShortArray -> "ShortArray(size=${value.size})"
    is IntArray -> "IntArray(size=${value.size})"
    is LongArray -> "LongArray(size=${value.size})"
    is FloatArray -> "FloatArray(size=${value.size})"
    is DoubleArray -> "DoubleArray(size=${value.size})"
    is Map.Entry<*, *> -> "${collectionDebugElementString(value.key)}=${collectionDebugElementString(value.value)}"
    else -> value.toString()
}

@UsedFromCompilerGeneratedCode
@TypedIntrinsic(IntrinsicType.IMMUTABLE_BLOB)
@Escapes.Nothing
internal external fun immutableBlobOfImpl(data: String): ImmutableBlob

@ExportForCppRuntime("Kotlin_internal_executeAndRelease")
internal fun executeAndRelease(actionRef: ExternalRCRef) {
    @Suppress("UNCHECKED_CAST")
    val action = dereferenceExternalRCRef(actionRef) as () -> Unit
    releaseExternalRCRef(actionRef)
    disposeExternalRCRef(actionRef)
    action()
}
