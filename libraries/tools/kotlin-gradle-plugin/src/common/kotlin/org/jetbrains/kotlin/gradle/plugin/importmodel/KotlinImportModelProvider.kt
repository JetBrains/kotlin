/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File

internal class KotlinImportModelProvider(
    private val project: Project,
) {
    fun baseInformation(): BaseModel = baseModel {
        id = KotlinImportModelIds.BASE
        pluginVersion = project.kotlinToolingVersion.toImportModelVersion()
        capabilities += Capability.CAPABILITY_JVM
    }

    fun projectInformation(): ProjectModel = projectModel {
        id = KotlinImportModelIds.PROJECT_INFORMATION
        compilationUnitIds += supportedCompilations().map { compilationUnitId(it.name) }
    }

    fun compilationUnit(id: CompilationUnitId): CompilationUnitModel {
        val compilation = supportedCompilations().singleOrNull { compilationUnitId(it.name) == id }
            ?: error("Unknown Kotlin import compilation unit '${id.value}' for project '${project.path}'")

        return compilationUnitModel {
            this.id = KotlinImportModelIds.COMPILATION_UNIT
            parameters = CompilationUnitModelKt.parameters { compilationUnitId = id }
            compilationName = compilation.name
            platform = Platform.PLATFORM_JVM
            isTest = compilation.name == KotlinCompilation.TEST_COMPILATION_NAME
            sourceRoots += sourceRoots(compilation)
            buildActions += gradleAction(compilation.compileTaskProvider.get().path)
        }
    }

    private fun supportedCompilations() = listOf(
        KotlinCompilation.MAIN_COMPILATION_NAME,
        KotlinCompilation.TEST_COMPILATION_NAME,
    ).map { name -> project.kotlinJvmExtension.target.compilations.getByName(name) }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun sourceRoots(compilation: KotlinCompilation<*>): List<SourceRoot> {
        fun roots(
            kind: SourceRootKind,
            paths: Iterable<File>,
            producingActions: (File) -> List<Action> = { emptyList() },
        ) = paths.map { path ->
            sourceRoot {
                this.path = project.relativePath(path).replace(File.separatorChar, '/')
                this.kind = kind
                this.producingActions += producingActions(path)
            }
        }

        val generatedKotlin = compilation.defaultSourceSet.generatedKotlin
        val generatedSourceProducers = generatedKotlin.buildDependencies.getDependencies(null)
        return (
                roots(
                    SourceRootKind.SOURCE_ROOT_KIND_SOURCE,
                    compilation.defaultSourceSet.kotlin.srcDirs
                ) + roots(
                    SourceRootKind.SOURCE_ROOT_KIND_GENERATED,
                    generatedKotlin.srcDirs
                ) { sourceRoot ->
                    generatedSourceProducers
                        .filter { sourceRoot in it.outputs.files.files }
                        .sortedBy { it.path }
                        .map { producer -> gradleAction(producer.path) }
                })
            .sortedWith(
                compareBy(SourceRoot::getPath)
                    .thenBy(SourceRoot::getKindValue)
            )
            .distinctBy(SourceRoot::getPath)
    }

    private fun gradleAction(taskPath: String): Action = action {
        gradleAction = gradleTaskAction { this.taskPath = taskPath }
    }

    private fun compilationUnitId(compilationName: String): CompilationUnitId {
        val buildPath = project.currentBuildId().compatAccessor(project).buildPath
        val targetKey = project.kotlinJvmExtension.target.targetName.ifEmpty { "jvm" }
        val value = listOf(buildPath, project.path, targetKey, compilationName).joinToString("|")
        return compilationUnitId { this.value = value }
    }
}

private fun KotlinToolingVersion.toImportModelVersion(): Version = version {
    major = this@toImportModelVersion.major
    minor = this@toImportModelVersion.minor
    patch = this@toImportModelVersion.patch
    this@toImportModelVersion.classifier?.let { classifier = it }
}
