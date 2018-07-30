/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

data class ScriptDiagnostic(
    val message: String,
    val severity: Severity = Severity.ERROR,
    val location: ScriptSource.Location? = null,
    val exception: Throwable? = null
) {
    enum class Severity { FATAL, ERROR, WARNING, INFO, DEBUG }
}

sealed class ResultWithDiagnostics<out R> {
    abstract val reports: List<ScriptDiagnostic>

    data class Success<out R>(
        val value: R,
        override val reports: List<ScriptDiagnostic> = listOf()
    ) : ResultWithDiagnostics<R>()

    data class Failure(
        override val reports: List<ScriptDiagnostic>
    ) : ResultWithDiagnostics<Nothing>() {
        constructor(vararg reports: ScriptDiagnostic) : this(reports.asList())
    }
}

// call chaining

suspend fun <R1, R2> ResultWithDiagnostics<R1>.onSuccess(body: suspend (R1) -> ResultWithDiagnostics<R2>): ResultWithDiagnostics<R2> =
    when (this) {
        is ResultWithDiagnostics.Success -> this.reports + body(this.value)
        is ResultWithDiagnostics.Failure -> this
    }

suspend fun <R> ResultWithDiagnostics<R>.onFailure(body: suspend (ResultWithDiagnostics<R>) -> Unit): ResultWithDiagnostics<R> {
    if (this is ResultWithDiagnostics.Failure) {
        body(this)
    }
    return this
}

operator fun <R> List<ScriptDiagnostic>.plus(res: ResultWithDiagnostics<R>): ResultWithDiagnostics<R> = when (res) {
    is ResultWithDiagnostics.Success -> ResultWithDiagnostics.Success(res.value, this + res.reports)
    is ResultWithDiagnostics.Failure -> ResultWithDiagnostics.Failure(this + res.reports)
}

// results creation

fun <R> R.asSuccess(reports: List<ScriptDiagnostic> = listOf()): ResultWithDiagnostics.Success<R> =
    ResultWithDiagnostics.Success(this, reports)

fun Throwable.asDiagnostics(customMessage: String? = null, location: ScriptSource.Location? = null): ScriptDiagnostic =
    ScriptDiagnostic(customMessage ?: message ?: "$this", ScriptDiagnostic.Severity.ERROR, location, this)

fun String.asErrorDiagnostics(location: ScriptSource.Location? = null): ScriptDiagnostic =
    ScriptDiagnostic(this, ScriptDiagnostic.Severity.ERROR, location)

fun<R> ResultWithDiagnostics<R>.resultOrNull(): R? = when (this) {
    is ResultWithDiagnostics.Success<R> -> value
    else -> null
}
