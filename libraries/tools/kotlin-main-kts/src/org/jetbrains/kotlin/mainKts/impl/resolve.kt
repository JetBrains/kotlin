/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts.impl

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.flatMapSuccess
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver
import kotlin.script.experimental.dependencies.tryAddRepository

suspend fun resolveFromAnnotations(resolver: ExternalDependenciesResolver, annotations: Iterable<Annotation>): ResultWithDiagnostics<List<File>> {
    annotations.forEach { annotation ->
        when (annotation) {
            is Repository -> {
                val repositoryCoordinates = with(annotation) { value.takeIf { it.isNotBlank() } ?: url }
                if (!resolver.tryAddRepository(repositoryCoordinates))
                    return makeFailureResult("Unrecognized repository coordinates: $repositoryCoordinates")
            }
            is DependsOn -> {}
            else -> return makeFailureResult("Unknown annotation ${annotation.javaClass}")
        }
    }
    return annotations.filterIsInstance(DependsOn::class.java).flatMapSuccess { dep ->
        val artifactCoordinates =
            if (dep.value.isNotBlank()) dep.value
            else listOf(dep.groupId, dep.artifactId, dep.version).filter { it?.isNotBlank() ?: false }.joinToString(":")
        resolver.resolve(artifactCoordinates)
    }
}

