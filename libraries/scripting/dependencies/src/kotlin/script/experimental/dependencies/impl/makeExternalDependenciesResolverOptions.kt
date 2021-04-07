/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.impl

import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver

fun makeExternalDependenciesResolverOptions(map: Map<String, String>): ExternalDependenciesResolver.Options =
    MapExternalDependenciesResolverOptions(map)

suspend fun ExternalDependenciesResolver.resolve(
    artifactCoordinates: String,
    options: Map<String, String>,
    sourceCodeLocation: SourceCode.LocationWithId? = null
): ResultWithDiagnostics<List<File>> = resolve(artifactCoordinates, makeExternalDependenciesResolverOptions(options), sourceCodeLocation)

private class MapExternalDependenciesResolverOptions(
    private val map: Map<String, String>
) : ExternalDependenciesResolver.Options {

    override fun value(name: String): String? = map[name]

    override fun flag(name: String): Boolean? = map[name]?.let { value ->
        when (value.lowercase()) {
            "1", "true", "t", "yes", "y" -> true
            "0", "false", "f", "no", "n" -> false
            else -> null
        }
    }
}