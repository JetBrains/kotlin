/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

internal class KotlinImportModelProvider(
    private val project: Project,
) {
    fun baseInformation(): BaseModel = baseModel {
        id = KotlinImportModelIds.BASE
        pluginVersion = project.kotlinToolingVersion.toImportModelVersion()
        capabilities += BaseModel.Capability.CAPABILITY_JVM
    }

    fun projectInformation(): ProjectModel = projectModel {
        id = KotlinImportModelIds.PROJECT_INFORMATION
        compilationUnitIds += supportedCompilations().map { compilationUnitId(it.name) }
    }

    fun compilationUnit(id: CompilationUnitId): CompilationUnitModel {
        val compilation = compilation(id)
        return compilationUnitModel {
            this.id = KotlinImportModelIds.COMPILATION_UNIT
            parameters = CompilationUnitModelKt.parameters { compilationUnitId = id }
            name = compilation.name
            platform = CompilationUnitModel.Platform.PLATFORM_JVM
            isTest = compilation.name == KotlinCompilation.TEST_COMPILATION_NAME
        }
    }

    private fun compilation(id: CompilationUnitId): KotlinCompilation<*> =
        supportedCompilations().singleOrNull { compilationUnitId(it.name) == id }
            ?: error("Unknown Kotlin import compilation unit '${id.value}' for project '${project.path}'")

    private fun supportedCompilations() = listOf(
        KotlinCompilation.MAIN_COMPILATION_NAME,
        KotlinCompilation.TEST_COMPILATION_NAME,
    ).map { name -> project.kotlinJvmExtension.target.compilations.getByName(name) }

    private fun compilationUnitId(compilationName: String): CompilationUnitId {
        val buildPath = project.currentBuildId().compatAccessor(project).buildPath
        val targetKey = project.kotlinJvmExtension.target.targetName.ifEmpty { "jvm" }
        // Stable opaque format: <build-path>|<project-path>|<target-key>|<compilation-name>
        return compilationUnitId { value = listOf(buildPath, project.path, targetKey, compilationName).joinToString("|") }
    }
}

private fun KotlinToolingVersion.toImportModelVersion(): Version = version {
    major = this@toImportModelVersion.major
    minor = this@toImportModelVersion.minor
    patch = this@toImportModelVersion.patch
    this@toImportModelVersion.classifier?.let { classifier = it }
}
