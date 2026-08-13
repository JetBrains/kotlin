/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.ide.IdeCompilerArgumentsResolver
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.gradle.utils.invariantSeparatorsPathString
import org.jetbrains.kotlin.gradle.utils.lenientArtifactsView
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

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
        val compileTask = compilation.compileTaskProvider.get() as KotlinCompile
        val compileAction = gradleAction(compileTask.path)
        return compilationUnitModel {
            this.id = KotlinImportModelIds.COMPILATION_UNIT
            parameters = CompilationUnitModelKt.parameters { compilationUnitId = id }
            name = compilation.name
            platform = CompilationUnitModel.Platform.PLATFORM_JVM
            isTest = compilation.name == KotlinCompilation.TEST_COMPILATION_NAME
            sourceRoots += sourceRoots(compilation)
            outputs += output(compileTask, compileAction)
        }
    }

    fun compilerArguments(id: CompilationUnitId): CompilerArgumentsModel {
        val compilation = compilation(id)
        return compilerArgumentsModel {
            this.id = KotlinImportModelIds.COMPILER_ARGUMENTS
            parameters = CompilerArgumentsModelKt.parameters { compilationUnitId = id }
            arguments += IdeCompilerArgumentsResolver.instance(project).resolveCompilerArguments(compilation).orEmpty()
        }
    }

    fun dependencies(parameters: DependenciesModel.Parameters): DependenciesModel {
        val compilation = compilation(parameters.compilationUnitId)
        val artifacts = compilation.internal.configurations.compileDependencyConfiguration.lenientArtifactsView

        return dependenciesModel {
            id = KotlinImportModelIds.DEPENDENCIES
            this.parameters = parameters
            val artifactsByFile = artifacts.artifacts.associateBy { it.file }
            binaryDependencies += artifacts.artifactFiles.files.mapNotNull { artifactsByFile[it]?.toBinaryDependency() }
            unresolvedDependencies += artifacts.failures.mapNotNull(::toUnresolvedDependency)
            sourceDependencies += sourceDependencies(compilation)
        }
    }

    private fun compilation(id: CompilationUnitId): KotlinCompilation<*> =
        supportedCompilations().singleOrNull { compilationUnitId(it.name) == id }
            ?: error("Unknown Kotlin import compilation unit '${id.value}' for project '${project.path}'")

    private fun supportedCompilations() = listOf(
        KotlinCompilation.MAIN_COMPILATION_NAME,
        KotlinCompilation.TEST_COMPILATION_NAME,
    ).map { name -> project.kotlinJvmExtension.target.compilations.getByName(name) }

    private fun sourceDependencies(compilation: KotlinCompilation<*>): List<DependenciesModel.SourceDependency> {
        val supportedCompilations = supportedCompilations()
        return compilation.associatedCompilations
            .filter { it in supportedCompilations }
            .sortedBy { compilationUnitId(it.name).value }
            .map { associatedCompilation ->
                DependenciesModelKt.sourceDependency {
                    kind = DependenciesModel.SourceDependencyKind.SOURCE_DEPENDENCY_KIND_FRIEND
                    targetCompilationUnitId = compilationUnitId(associatedCompilation.name)
                }
            }
    }

    private fun ResolvedArtifactResult.toBinaryDependency(): BinaryDependency? = when (val component = id.componentIdentifier) {
        is ModuleComponentIdentifier -> binaryDependency {
            coordinates = mavenCoordinates {
                group = component.group
                module = component.module
                version = component.version
            }
            artifactPath = file.absolutePath
        }
        is ProjectComponentIdentifier -> {
            // TODO: Support project-to-project dependencies
            null
        }
        else -> binaryDependency {
            artifactPath = file.absolutePath
        }
    }

    private fun toUnresolvedDependency(failure: Throwable): DependenciesModel.Unresolved? {
        val selector = (failure as? ModuleVersionResolveException)?.selector as? ModuleComponentSelector ?: return null
        return DependenciesModelKt.unresolved {
            coordinates = mavenCoordinates {
                group = selector.group
                module = selector.module
                version = selector.version
            }
            failureMessage = runCatching { failure.message }.getOrNull()
                ?: "Failed to resolve dependency"
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun sourceRoots(compilation: KotlinCompilation<*>): List<SourceRoot> {
        fun roots(
            kind: SourceRoot.Kind,
            paths: Iterable<Path>,
            producingActions: (Path) -> List<Action> = { emptyList() },
        ) = paths.map { path ->
            sourceRoot {
                this.path = project.relativeProjectPath(path)
                this.kind = kind
                this.producingActions += producingActions(path)
            }
        }

        val generatedKotlin = compilation.defaultSourceSet.generatedKotlin
        val tasks = project.tasks
        return (
            roots(SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE, compilation.defaultSourceSet.kotlin.srcDirs.map(File::toPath)) +
                roots(SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED, generatedKotlin.srcDirs.map(File::toPath)) { sourceRoot ->
                    tasks
                        .filter { sourceRoot in it.outputs.files.files.map(File::toPath) }
                        .sortedBy { it.path }
                        .map { producer -> gradleAction(producer.path) }
                }
            )
            .sortedWith(compareBy(SourceRoot::getPath).thenBy(SourceRoot::getKindValue))
            .distinctBy(SourceRoot::getPath)
    }

    private fun output(compileTask: KotlinCompile, producingAction: Action): CompilationUnitModel.Output = CompilationUnitModelKt.output {
        path = project.projectDir.toPath()
            .relativize(compileTask.destinationDirectory.get().asFile.toPath())
            .invariantSeparatorsPathString
        producingActions += producingAction
    }

    private fun gradleAction(taskPath: String): Action = action {
        gradleAction = ActionKt.gradleTask { this.taskPath = taskPath }
    }

    private fun Project.relativeProjectPath(path: Path): String =
        Paths.get(relativePath(path.toFile())).invariantSeparatorsPathString

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
