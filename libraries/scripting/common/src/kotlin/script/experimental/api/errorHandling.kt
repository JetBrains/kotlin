/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

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
    val location: SourceCode.Location? = null,
    val exception: Throwable? = null
) {
    /**
     * The diagnostic severity
     */
    enum class Severity { FATAL, ERROR, WARNING, INFO, DEBUG }
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
}

/**
 * Chains actions on successful result:
 * If receiver is success - executes [body] and merge diagnostic reports
 * otherwise returns the failure as is
 */
suspend fun <R1, R2> ResultWithDiagnostics<R1>.onSuccess(body: suspend (R1) -> ResultWithDiagnostics<R2>): ResultWithDiagnostics<R2> =
    when (this) {
        is ResultWithDiagnostics.Success -> this.reports + body(this.value)
        is ResultWithDiagnostics.Failure -> this
    }

/**
 * Chains actions on failure:
 * If receiver is failure - executed [body]
 * otherwise returns the receiver as is
 */
suspend fun <R> ResultWithDiagnostics<R>.onFailure(body: suspend (ResultWithDiagnostics<R>) -> Unit): ResultWithDiagnostics<R> {
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
 * Converts the receiver Throwable to the Failure results wrapper with optional [message] and [location]
 */
fun Throwable.asDiagnostics(customMessage: String? = null, location: SourceCode.Location? = null): ScriptDiagnostic =
    ScriptDiagnostic(customMessage ?: message ?: "$this", ScriptDiagnostic.Severity.ERROR, location, this)

/**
 * Converts the receiver String to error diagnostic report with optional [location]
 */
fun String.asErrorDiagnostics(location: SourceCode.Location? = null): ScriptDiagnostic =
    ScriptDiagnostic(this, ScriptDiagnostic.Severity.ERROR, location)

/**
 * Extracts the result value from the receiver wrapper or null if receiver represents a Failure
 */
fun<R> ResultWithDiagnostics<R>.resultOrNull(): R? = when (this) {
    is ResultWithDiagnostics.Success<R> -> value
    else -> null
}
