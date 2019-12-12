package org.jetbrains.kotlin.tools.projectWizard.core

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume


sealed class TaskResult<out T : Any>
data class Success<T : Any>(val value: T) : TaskResult<T>()
data class Failure(val errors: List<Error>) : TaskResult<Nothing>() {
    constructor(vararg errors: Error) : this(errors.toList())
}

val TaskResult<Any>.isSuccess
    get() = this is Success<*>

fun <T : Any> success(value: T): TaskResult<T> =
    Success(value)

fun <T : Any> failure(vararg errors: Error): TaskResult<T> =
    Failure(*errors)

val <T : Any> TaskResult<T>.asNullable: T?
    get() = safeAs<Success<T>>()?.value

inline fun <T : Any> TaskResult<T>.onSuccess(action: (T) -> Unit) = also {
    if (this is Success<T>) {
        action(value)
    }
}

inline fun <T : Any> TaskResult<T>.onFailure(handler: (List<Error>) -> Unit) = apply {
    if (this is Failure) {
        handler(errors)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <A : Any, B : Any, R : Any> TaskResult<A>.mappend(
    other: TaskResult<B>,
    op: (A, B) -> R
): TaskResult<R> = mappendM(other) { a, b -> success(op(a, b)) }

@Suppress("UNCHECKED_CAST")
inline fun <A : Any, B : Any, R : Any> TaskResult<A>.mappendM(
    other: TaskResult<B>,
    op: (A, B) -> TaskResult<R>
): TaskResult<R> = when {
    this is Success<*> && other is Success<*> -> op(value as A, other.value as B)
    this is Success<*> && other is Failure -> other
    this is Failure && other is Success<*> -> this
    this is Failure && other is Failure -> Failure(errors + other.errors)
    else -> error("Can not happen")
}

infix fun <T : Any> TaskResult<*>.andThen(other: TaskResult<T>): TaskResult<T> =
    mappendM(other) { _, _ -> other }

operator fun <T : Any> TaskResult<List<T>>.plus(other: TaskResult<List<T>>): TaskResult<Unit> =
    mappend(other) { _, _ -> Unit }


fun <T : Any> T?.toResult(failure: () -> Error): TaskResult<T> =
    this?.let { Success(it) } ?: Failure(
        failure()
    )

fun <T : Any> T.asSuccess(): Success<T> =
    Success(this)

inline fun <T : Any> TaskResult<T>.raise(returnExpression: (Failure) -> (Unit)): T =
    when (this) {
        is Failure -> {
            returnExpression(this)
            error("fail should return out of the function")
        }
        is Success -> value
    }


fun <T : Any> Iterable<TaskResult<T>>.sequence(): TaskResult<List<T>> =
    fold(success(emptyList())) { acc, result ->
        acc.mappend(result) { a, r -> a + r }
    }

fun <T : Any> Iterable<TaskResult<T>>.sequenceIgnore(): TaskResult<Unit> =
    fold<TaskResult<T>, TaskResult<Unit>>(UNIT_SUCCESS) { acc, result ->
        acc.mappend(result) { _, _ -> Unit }
    }

fun <T : Any> Iterable<T>.mapSequenceIgnore(f: (T) -> TaskResult<*>): TaskResult<Unit> =
    fold<T, TaskResult<Unit>>(UNIT_SUCCESS) { acc, result ->
        acc.mappend(f(result)) { _, _ -> Unit }
    }

fun <T : Any, R : Any> Iterable<T>.mapSequence(f: (T) -> TaskResult<R>): TaskResult<List<R>> =
    map(f).sequence()

fun <T : Any> Sequence<TaskResult<T>>.sequenceFailFirst(): TaskResult<List<T>> =
    fold(success(emptyList())) { acc, result ->
        if (acc.isSuccess) acc.mappend(result) { a, r -> a + r }
        else acc
    }

fun <T : Any, R : Any> TaskResult<T>.map(f: (T) -> R): TaskResult<R> = when (this) {
    is Failure -> this
    is Success<T> -> Success(f(value))
}

fun <T : Any> TaskResult<T>.mapFailure(f: (List<Error>) -> List<Error>): TaskResult<T> = when (this) {
    is Failure -> Failure(f(errors))
    is Success<T> -> this
}

fun <T : Any> TaskResult<T>.recover(f: (Failure) -> TaskResult<T>): TaskResult<T> = when (this) {
    is Failure -> f(this)
    is Success<T> -> this
}

fun <T : Any, R : Any> TaskResult<T>.flatMap(f: (T) -> TaskResult<R>): TaskResult<R> = when (this) {
    is Failure -> this
    is Success<T> -> f(value)
}

fun <T : Any> TaskResult<T>.withAssert(assertion: (T) -> Error?): TaskResult<T> = when (this) {
    is Failure -> this
    is Success<T> -> assertion(value)?.let { Failure(it) } ?: this
}


fun <T : Any> TaskResult<T>.getOrElse(default: () -> T): T = when (this) {
    is Success<T> -> value
    is Failure -> default()
}

fun <T : Any> TaskResult<T>.ignore(): TaskResult<Unit> = when (this) {
    is Failure -> this
    is Success<T> -> UNIT_SUCCESS
}

val UNIT_SUCCESS = Success(Unit)
