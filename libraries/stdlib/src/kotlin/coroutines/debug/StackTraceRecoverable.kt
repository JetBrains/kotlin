package kotlin.coroutines.debug

import kotlin.coroutines.ExperimentalStdlibCoroutineSupportApi

/**
 * A [Throwable] that is aware of stacktrace recovery and explicitly supports
 * a procedure for copying it.
 *
 * Whenever an exception object is created in one concurrent computation,
 * stored in some shared memory, and then accessed by and rethrown in another
 * concurrent computation, the stacktrace of rethrowing computation is going
 * to be absent from the exception's stacktrace.
 *
 * To work around this, asynchronous frameworks may establish a convention where
 * an exception received from a different concurrent computation is getting
 * *copied* first, with the copy getting populated with the stacktrace of the
 * caller and only then rethrown.
 *
 * In `kotlinx.coroutines`, there is such a convention available on the JVM as
 * an opt-in, called "stacktrace recovery".
 * It is available by default to all exception classes that have one of the
 * following constructors:
 * - `(String, Throwable?)`
 * - `(String)`
 * - `(Throwable?)`
 * - a constructor with no parameters.
 *
 * Implementing [StackTraceRecoverable] in a [Throwable] subclass allows
 * its instances to be copied for stacktrace recovery when an asynchronous
 * framework can not find a suitable constructor.
 *
 * Alternatively, this interface can be used to opt out
 * from stacktrace recovery even in scenarios when an asynchronous framework
 * can heuristically find a copying procedure.
 * To that end, [copyForStackTraceRecovery] needs to return `null`.
 *
 * In `kotlinx.coroutines`, the copying mechanism is only available on the JVM,
 * but this interface is available on all targets so that exceptions
 * implemented in common code can also support stacktrace recovery on the JVM.
 *
 * ### Implementing `StackTraceRecoverable`
 *
 * Consider this class:
 *
 * ```
 * class BadResponseCodeException(val responseCode: Int): Exception()
 * ```
 *
 * Since it only has a constructor that isn't recognized by `kotlinx.coroutines`,
 * its stack traces will be left untouched.
 *
 * On the JVM, implementing [StackTraceRecoverable] only requires adding a type bound and
 * implementing the new method:
 *
 * ```
 * class BadResponseCodeException(
 *     val responseCode: Int
 * ): Exception(), StackTraceRecoverable<BadResponseCodeException> {
 *     override fun copyForStackTraceRecovery(): BadResponseCodeException {
 *         val result = BadResponseCodeException(responseCode)
 *         result.initCause(this)
 *         return result
 *     }
 * }
 * ```
 *
 * In multiplatform code, `initCause` is not available, so it is more diffcult
 * to fulfill the recommended pattern of keeping the original exception
 * as the [cause][Throwable.cause].
 * A separate private constructor is required to achieve that:
 *
 * ```
 * class BadResponseCodeException private constructor(
 *     val responseCode: Int,
 *     cause: Throwable?
 * ) : Exception(cause), StackTraceRecoverable<BadResponseCodeException> {
 *
 *     // The original public constructor
 *     constructor(responseCode: Int): this(responseCode, null)
 *
 *     override fun copyForStackTraceRecovery(): BadResponseCodeException =
 *         BadResponseCodeException(responseCode, this)
 * }
 * ```
 */
@SinceKotlin("2.4")
@ExperimentalStdlibCoroutineSupportApi
public interface StackTraceRecoverable<T>
where T: Throwable, T: StackTraceRecoverable<T> {
    /**
     * Creates a copy of `this` for stacktrace recovery.
     *
     * For better debuggability, it is recommended to use original exception as
     * the [cause][Throwable.cause] of the resulting one.
     * The stack trace of the copied exception will be overwritten by
     * stacktrace recovery machinery using the `Throwable.setStackTrace` call.
     *
     * An exception can opt-out of copying by returning `null` from this function.
     *
     * Suppressed exceptions of the original exception should be left uncopied,
     * to avoid circular exceptions.
     *
     * This function may create a copy with a modified [message][Throwable.message],
     * but note that the copy can be later recovered as well,
     * so the message modification code should handle this situation correctly
     * (e.g. by also storing the original message and checking it)
     * to produce a human-readable result.
     */
    public fun copyForStackTraceRecovery(): T?
}
