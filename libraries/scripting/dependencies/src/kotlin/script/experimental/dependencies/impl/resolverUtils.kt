/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.impl

import java.net.MalformedURLException
import java.net.URL
import kotlin.script.experimental.dependencies.RepositoryCoordinates
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode

fun makeResolveFailureResult(message: String, location: SourceCode.LocationWithId? = null) =
    makeResolveFailureResult(listOf(message), location)

fun makeResolveFailureResult(messages: Iterable<String>, location: SourceCode.LocationWithId? = null) =
    makeResolveFailureResult(messages, location, null)

fun makeResolveFailureResult(messages: Iterable<String>, location: SourceCode.LocationWithId?, throwable: Throwable?) =
    ResultWithDiagnostics.Failure(
        messages.map {
            ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, it, ScriptDiagnostic.Severity.WARNING, location, throwable)
        }
    )

fun RepositoryCoordinates.toRepositoryUrlOrNull(): URL? =
    try {
        URL(string)
    } catch (_: MalformedURLException) {
        null
    }
