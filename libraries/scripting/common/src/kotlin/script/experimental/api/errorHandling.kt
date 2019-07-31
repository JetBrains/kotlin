/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.RuntimeException

/**
 * The single script diagnostic report
 * @param message diagnostic message
 * @param severity diagnostic severity ({@link ScriptDiagnostic#Severity})
 * @param location optional source location for the diagnostic
 * @param exception optional exception caused the diagnostic
 */
data class ScriptDiagnostic(
    val message: String,
    val severity: Severity = Severity.ERROR,
    val sourcePath: String? = null,
    val location: SourceCode.Location? = null,
    val exception: Throwable? = null
) {
    /**
     * The diagnostic severity
     */
    enum class Severity { FATAL, ERROR, WARNING, INFO, DEBUG }

    override fun toString(): String = render()

    /**
     * Render diagnostics message as a string in a form:
     * "[SEVERITY ]message[ (file:line:column)][: exception message[\n exception stacktrace]]"
     * @param withSeverity add severity prefix, true by default
     * @param withLocation add error location in the compiled script, if present, true by default
     * @param withException add exception message, if present, true by default
     * @param withStackTrace add exception stacktrace, if exception is present and [withException] is true, false by default
     */
    fun render(
        withSeverity: Boolean = true,
        withLocation: Boolean = true,
        withException: Boolean = true,
        withStackTrace: Boolean = false
    ): String = buildString {
        if (withSeverity) {
            append(severity.name)
            append(' ')
        }
        append(message)
        if (withLocation && (sourcePath != null || location != null)) {
            append(" (")
            sourcePath?.let { append(it.substringAfterLast(File.separatorChar)) }
            location?.let {
                append(':')
                append(it.start.line)
                append(':')
                append(it.start.col)
            }
            append(')')
        }
        if (withException && exception != null) {
            append(": ")
            append(exception)
            if (withStackTrace) {
                ByteArrayOutputStream().use { os ->
                    val ps = PrintStream(os)
                    exception.printStackTrace(ps)
                    ps.flush()
                    append("\n")
                    append(os.toString())
                }
            }
        }
    }
}

/**
 * The result wrapper with diagnostics container
 */
sealed class ResultWithDiagnostics<out R> {
    /**
     * The diagnostic reports container
     */
    abstract val reports: List<ScriptDiagnostic>

    /**
     * The successful [value] result with optional [reports] with diagnostics
     */
    data class Success<out R>(
        val value: R,
        override val reports: List<ScriptDiagnostic> = listOf()
    ) : ResultWithDiagnostics<R>()

    /**
     * The class representing the failure result
     * @param reports diagnostics associated with the failure
     */
    data class Failure(
        override val reports: List<ScriptDiagnostic>
    ) : ResultWithDiagnostics<Nothing>() {
        constructor(vararg reports: ScriptDiagnostic) : this(reports.asList())
    }

    override fun equals(other: Any?): Boolean = this === other || (other is ResultWithDiagnostics<*> && this.reports == other.reports)

    override fun hashCode(): Int = reports.hashCode()
}

/**
 * Chains actions on successful result:
 * If receiver is success - executes [body] and merge diagnostic reports
 * otherwise returns the failure as is
 */
inline fun <R1, R2> ResultWithDiagnostics<R1>.onSuccess(body: (R1) -> ResultWithDiagnostics<R2>): ResultWithDiagnostics<R2> =
    when (this) {
        is ResultWithDiagnostics.Success -> this.reports + body(this.value)
        is ResultWithDiagnostics.Failure -> this
    }

/**
 * maps transformation ([body]) over iterable merging diagnostics
 * return failure with merged diagnostics after first failed transformation
 * and success with merged diagnostics and list of results if all transformations succeeded
 */
inline fun<T, R> Iterable<T>.mapSuccess(body: (T) -> ResultWithDiagnostics<R>): ResultWithDiagnostics<List<R>> {
    val reports = ArrayList<ScriptDiagnostic>()
    val results = ArrayList<R>()
    for (it in this) {
        val result = body(it)
        reports.addAll(result.reports)
        when (result) {
            is ResultWithDiagnostics.Success -> {
                results.add(result.value)
            }
            else -> {
                return ResultWithDiagnostics.Failure(reports)
            }
        }
    }
    return results.asSuccess(reports)
}

/**
 * Chains actions on failure:
 * If receiver is failure - executed [body]
 * otherwise returns the receiver as is
 */
inline fun <R> ResultWithDiagnostics<R>.onFailure(body: (ResultWithDiagnostics<R>) -> Unit): ResultWithDiagnostics<R> {
    if (this is ResultWithDiagnostics.Failure) {
        body(this)
    }
    return this
}

/**
 * Merges diagnostics report with the [result] wrapper
 */
operator fun <R> List<ScriptDiagnostic>.plus(result: ResultWithDiagnostics<R>): ResultWithDiagnostics<R> = when (result) {
    is ResultWithDiagnostics.Success -> ResultWithDiagnostics.Success(result.value, this + result.reports)
    is ResultWithDiagnostics.Failure -> ResultWithDiagnostics.Failure(this + result.reports)
}

/**
 * Converts the receiver value to the Success result wrapper with optional diagnostic [reports]
 */
fun <R> R.asSuccess(reports: List<ScriptDiagnostic> = listOf()): ResultWithDiagnostics.Success<R> =
    ResultWithDiagnostics.Success(this, reports)

/**
 * Makes Failure result with optional diagnostic [reports]
 */
fun makeFailureResult(reports: List<ScriptDiagnostic>): ResultWithDiagnostics.Failure =
    ResultWithDiagnostics.Failure(reports)

/**
 * Makes Failure result with optional diagnostic [reports]
 */
fun makeFailureResult(vararg reports: ScriptDiagnostic): ResultWithDiagnostics.Failure =
    ResultWithDiagnostics.Failure(reports.asList())

/**
 * Makes Failure result with diagnostic [message] with optional [path] and [location]
 */
fun makeFailureResult(message: String, path: String? = null, location: SourceCode.Location? = null): ResultWithDiagnostics.Failure =
    ResultWithDiagnostics.Failure(message.asErrorDiagnostics(path, location))

/**
 * Converts the receiver Throwable to the Failure results wrapper with optional [customMessage], [path] and [location]
 */
fun Throwable.asDiagnostics(
    customMessage: String? = null,
    path: String? = null,
    location: SourceCode.Location? = null,
    severity: ScriptDiagnostic.Severity = ScriptDiagnostic.Severity.ERROR
): ScriptDiagnostic =
    ScriptDiagnostic(customMessage ?: message ?: "$this", severity, path, location, this)

/**
 * Converts the receiver String to error diagnostic report with optional [path] and [location]
 */
fun String.asErrorDiagnostics(path: String? = null, location: SourceCode.Location? = null): ScriptDiagnostic =
    ScriptDiagnostic(this, ScriptDiagnostic.Severity.ERROR, path, location)

/**
 * Extracts the result value from the receiver wrapper or null if receiver represents a Failure
 */
fun <R> ResultWithDiagnostics<R>.valueOrNull(): R? = when (this) {
    is ResultWithDiagnostics.Success<R> -> value
    else -> null
}

/**
 * Extracts the result value from the receiver wrapper or run non-returning lambda if receiver represents a Failure
 */
inline fun <R> ResultWithDiagnostics<R>.valueOr(body: (ResultWithDiagnostics.Failure) -> Nothing): R = when (this) {
    is ResultWithDiagnostics.Success<R> -> value
    is ResultWithDiagnostics.Failure -> body(this)
}

/**
 * Extracts the result value from the receiver wrapper or throw RuntimeException with diagnostics
 */
fun <R> ResultWithDiagnostics<R>.valueOrThrow(): R = valueOr {
    throw RuntimeException(
        reports.joinToString("\n") { it.exception?.toString() ?: it.message },
        reports.find { it.exception != null }?.exception
    )
}

