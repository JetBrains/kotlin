/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

@file:Suppress("DEPRECATION", "DEPRECATION_ERROR") // Char.toInt()
package kotlin.native.internal

import kotlin.experimental.ExperimentalNativeApi
import kotlin.internal.getProgressionLastElement
import kotlin.reflect.KClass
import kotlin.concurrent.AtomicReference
import kotlinx.cinterop.*

@ExportForCppRuntime
@PublishedApi
internal fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowIndexOutOfBoundsException(): Nothing {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    @Suppress("DEPRECATION")
    throw ArrayIndexOutOfBoundsException()
}

@ExportForCppRuntime
@PublishedApi
internal fun ThrowClassCastException(instance: Any, typeInfo: NativePtr): Nothing {
    val clazz = KClassImpl<Any>(typeInfo)
    throw ClassCastException("${instance::class} cannot be cast to $clazz")
}

@ExportForCppRuntime
@PublishedApi
internal fun ThrowTypeCastException(instance: Any, typeName: String): Nothing {
    throw TypeCastException("${instance::class} cannot be cast to class $typeName")
}

@ExportForCppRuntime
@PublishedApi
internal fun ThrowKotlinNothingValueException(): Nothing {
    throw KotlinNothingValueException()
}

@ExportForCppRuntime
@PublishedApi
internal fun ThrowInvalidReceiverTypeException(klass: KClass<*>): Nothing {
    throw RuntimeException("Unexpected receiver type: " + (klass.qualifiedName ?: "noname"))
}

@ExportForCppRuntime
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

@PublishedApi
internal fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

@PublishedApi
internal fun ThrowUninitializedPropertyAccessException(propertyName: String): Nothing {
    throw UninitializedPropertyAccessException("lateinit property $propertyName has not been initialized")
}

@ExportForCppRuntime
internal fun ThrowIllegalArgumentException() : Nothing {
    throw IllegalArgumentException()
}

@ExportForCppRuntime
internal fun ThrowIllegalArgumentExceptionWithMessage(message: String) : Nothing {
    throw IllegalArgumentException(message)
}

@ExportForCppRuntime
internal fun ThrowIllegalStateException() : Nothing {
    throw IllegalStateException()
}

@ExportForCppRuntime
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

@ExportForCppRuntime
@FreezingIsDeprecated
internal fun ThrowIncorrectDereferenceException() {
    throw IncorrectDereferenceException(
            "Trying to access top level value not marked as @ThreadLocal or @SharedImmutable from non-main thread")
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

internal class IrLinkageError(message: String?) : Error(message)

@PublishedApi
internal fun ThrowIrLinkageError(message: String?): Nothing {
    throw IrLinkageError(message)
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
@OptIn(FreezingIsDeprecated::class, ExperimentalNativeApi::class)
internal object UnhandledExceptionHookHolder {
    internal val hook: AtomicReference<ReportUnhandledExceptionHook?> = AtomicReference<ReportUnhandledExceptionHook?>(null)
}

// TODO: Can be removed only when native-mt coroutines stop using it.
@PublishedApi
@ExportForCppRuntime
@OptIn(FreezingIsDeprecated::class, ExperimentalNativeApi::class)
internal fun OnUnhandledException(throwable: Throwable) {
    val handler = UnhandledExceptionHookHolder.hook.value
    if (handler == null) {
        ReportUnhandledException(throwable);
        return
    }
    try {
        handler(throwable)
    } catch (t: Throwable) {
        ReportUnhandledException(t)
    }
}

@ExportForCppRuntime("Kotlin_runUnhandledExceptionHook")
@OptIn(FreezingIsDeprecated::class, ExperimentalNativeApi::class)
internal fun runUnhandledExceptionHook(throwable: Throwable) {
    val handler = UnhandledExceptionHookHolder.hook.value ?: throw throwable
    handler(throwable)
}

@ExportForCppRuntime
internal fun TheEmptyString() = ""

@PublishedApi
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

@PublishedApi
internal fun <T: Enum<T>> valuesForEnum(values: Array<T>): Array<T> {
    val result = @Suppress("TYPE_PARAMETER_AS_REIFIED") Array<T?>(values.size)
    for (value in values)
        result[value.ordinal] = value
    @Suppress("UNCHECKED_CAST")
    return result as Array<T>
}

@PublishedApi
@TypedIntrinsic(IntrinsicType.CREATE_UNINITIALIZED_INSTANCE)
internal external fun <T> createUninitializedInstance(): T

@PublishedApi
@TypedIntrinsic(IntrinsicType.INIT_INSTANCE)
internal external fun initInstance(thiz: Any, constructorCall: Any): Unit

@PublishedApi
@TypedIntrinsic(IntrinsicType.IS_SUBTYPE)
internal external fun <T> isSubtype(objTypeInfo: NativePtr): Boolean

@PublishedApi
internal fun checkProgressionStep(step: Int): Int =
        if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")
@PublishedApi
internal fun checkProgressionStep(step: Long): Long =
        if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")

@PublishedApi
internal fun getProgressionLast(start: Char, end: Char, step: Int): Char =
        getProgressionLast(start.code, end.code, step).toChar()

@PublishedApi
internal fun getProgressionLast(start: Int, end: Int, step: Int): Int =
        getProgressionLastElement(start, end, step)
@PublishedApi
internal fun getProgressionLast(start: Long, end: Long, step: Long): Long =
        getProgressionLastElement(start, end, step)

@PublishedApi
// Called by the debugger.
@ExportForCppRuntime
internal fun KonanObjectToUtf8Array(value: Any?): ByteArray {
    val string = try {
        when (value) {
            is Array<*> -> value.contentToString()
            is CharArray -> value.contentToString()
            is BooleanArray -> value.contentToString()
            is ByteArray -> value.contentToString()
            is ShortArray -> value.contentToString()
            is IntArray -> value.contentToString()
            is LongArray -> value.contentToString()
            is FloatArray -> value.contentToString()
            is DoubleArray -> value.contentToString()
            else -> value.toString()
        }
    } catch (error: Throwable) {
        "<Thrown $error when converting to string>"
    }
    return string.encodeToByteArray()
}
