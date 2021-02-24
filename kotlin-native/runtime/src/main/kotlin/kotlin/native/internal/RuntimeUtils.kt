/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.internal.getProgressionLastElement
import kotlin.reflect.KClass
import kotlin.native.concurrent.AtomicReference

@ExportForCppRuntime
fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowIndexOutOfBoundsException(): Nothing {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    throw ArrayIndexOutOfBoundsException()
}

@ExportForCppRuntime
fun ThrowClassCastException(instance: Any, typeInfo: NativePtr): Nothing {
    val clazz = KClassImpl<Any>(typeInfo)
    throw ClassCastException("${instance::class.qualifiedName} cannot be cast to ${clazz.qualifiedName}")
}

@ExportForCppRuntime
fun ThrowTypeCastException(): Nothing {
    throw TypeCastException()
}

@ExportForCppRuntime
fun ThrowKotlinNothingValueException(): Nothing {
    throw KotlinNothingValueException()
}

@ExportForCppRuntime
fun ThrowInvalidReceiverTypeException(klass: KClass<*>): Nothing {
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

fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

fun ThrowUninitializedPropertyAccessException(propertyName: String): Nothing {
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
internal fun ThrowIncorrectDereferenceException() {
    throw IncorrectDereferenceException(
            "Trying to access top level value not marked as @ThreadLocal or @SharedImmutable from non-main thread")
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

@SymbolName("TerminateWithUnhandledException")
internal external fun TerminateWithUnhandledException(throwable: Throwable)

// Using object to make sure that `hook` is initialized when it's needed instead of
// in a normal global initialization flow. This is important if some global happens
// to throw an exception during it's initialization before this hook would've been initialized.
internal object UnhandledExceptionHookHolder {
    internal val hook: AtomicReference<ReportUnhandledExceptionHook?> = AtomicReference(null)
}

@PublishedApi
@ExportForCppRuntime
internal fun OnUnhandledException(throwable: Throwable) {
    val handler = UnhandledExceptionHookHolder.hook.swap(null)
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

@ExportForCppRuntime
internal fun TheEmptyString() = ""

public fun <T: Enum<T>> valueOfForEnum(name: String, values: Array<T>) : T {
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
    throw Exception("Invalid enum value name: $name")
}

public fun <T: Enum<T>> valuesForEnum(values: Array<T>): Array<T> {
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
internal fun checkProgressionStep(step: Int)  =
        if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")
@PublishedApi
internal fun checkProgressionStep(step: Long) =
        if (step > 0) step else throw IllegalArgumentException("Step must be positive, was: $step.")

@PublishedApi
internal fun getProgressionLast(start: Char, end: Char, step: Int): Char =
        getProgressionLast(start.toInt(), end.toInt(), step).toChar()

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

@TypedIntrinsic(IntrinsicType.LIST_OF_INTERNAL)
@PublishedApi
internal fun <T> listOfInternal(vararg elements: T): List<T> {
    val result = ArrayList<T>(elements.size)
    for (i in 0 until elements.size)
        result.add(elements[i])
    return result
}
