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
    fun baseInformation(): BaseModel = BaseModel.newBuilder()
        .setId(KotlinImportModelIds.BASE)
        .setPluginVersion(project.kotlinToolingVersion.toImportModelVersion())
        .addCapabilities(Capability.CAPABILITY_JVM)
        .build()

    fun projectInformation(): ProjectModel = ProjectModel.newBuilder()
        .setId(KotlinImportModelIds.PROJECT_INFORMATION)
        .addAllCompilationUnitIds(supportedCompilations().map { compilationUnitId(it.name) })
        .build()

    fun compilationUnit(id: CompilationUnitId): CompilationUnitModel {
        val compilation = supportedCompilations().singleOrNull { compilationUnitId(it.name) == id }
            ?: error("Unknown Kotlin import compilation unit '${id.value}' for project '${project.path}'")

        return CompilationUnitModel.newBuilder()
            .setId(KotlinImportModelIds.COMPILATION_UNIT)
            .setParameters(
                CompilationUnitModel.Parameters.newBuilder()
                    .setCompilationUnitId(id)
            )
            .setCompilationName(compilation.name)
            .setPlatform(Platform.PLATFORM_JVM)
            .setIsTest(compilation.name == KotlinCompilation.TEST_COMPILATION_NAME)
            .addAllSourceRoots(sourceRoots(compilation))
            .addBuildActions(gradleAction(compilation.compileTaskProvider.get().path))
            .build()
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
            SourceRoot.newBuilder()
                .setPath(project.relativePath(path).replace(File.separatorChar, '/'))
                .setKind(kind)
                .addAllProducingActions(producingActions(path))
                .build()
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

    private fun gradleAction(taskPath: String): Action = Action.newBuilder()
        .setGradleAction(GradleTaskAction.newBuilder().setTaskPath(taskPath))
        .build()

    private fun compilationUnitId(compilationName: String): CompilationUnitId {
        val buildPath = project.currentBuildId().compatAccessor(project).buildPath
        val targetKey = project.kotlinJvmExtension.target.targetName.ifEmpty { "jvm" }
        val value = listOf(buildPath, project.path, targetKey, compilationName).joinToString("|")
        return CompilationUnitId.newBuilder().setValue(value).build()
    }
}

private fun KotlinToolingVersion.toImportModelVersion(): Version = Version.newBuilder()
    .setMajor(major)
    .setMinor(minor)
    .setPatch(patch)
    .apply { this@toImportModelVersion.classifier?.let(::setClassifier) }
    .build()
