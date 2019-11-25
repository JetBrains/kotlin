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

fun makeResolveFailureResult(message: String) = makeResolveFailureResult(listOf(message))

fun makeResolveFailureResult(messages: Iterable<String>) =
    ResultWithDiagnostics.Failure(messages.map { ScriptDiagnostic(it, ScriptDiagnostic.Severity.WARNING) })

fun RepositoryCoordinates.toRepositoryUrlOrNull(): URL? =
    try {
        URL(string)
    } catch (_: MalformedURLException) {
        null
    }
