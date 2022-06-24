/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceSpec
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import java.io.File

object SourceSetMetadataStorageForIde {
    private fun getStorageRoot(project: Project): File = project.rootDir.resolve(".gradle/kotlin/sourceSetMetadata")

    private fun projectStorage(project: Project): File {
        val projectPathSegments = generateSequence(project) { it.parent }.map { it.name }
        return getStorageRoot(project).resolve(
            // Escape dots in project names to avoid ambiguous paths.
            projectPathSegments.joinToString(".") { it.replace(".", "_.") }
        )
    }

    fun sourceSetStorage(project: Project, sourceSetName: String) = projectStorage(project).resolve(sourceSetName)

    internal fun sourceSetStorageWithScope(project: Project, sourceSetName: String, scope: KotlinDependencyScope) =
        sourceSetStorage(project, sourceSetName).resolve(scope.scopeName)
}
