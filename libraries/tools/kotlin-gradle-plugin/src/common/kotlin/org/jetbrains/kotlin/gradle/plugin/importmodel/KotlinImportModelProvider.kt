/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.importmodel

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.tasks.TaskDependency
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.DATA_PATH
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.internal.compatAccessor
import org.jetbrains.kotlin.gradle.plugin.ide.IdeCompilerArgumentsResolver
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.gradle.utils.invariantSeparatorsPathString
import org.jetbrains.kotlin.gradle.utils.lenientArtifactsView
import org.jetbrains.kotlin.importmodels.KotlinImportModelIds
import org.jetbrains.kotlin.importmodels.proto.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import java.nio.file.Paths

internal class KotlinImportModelProvider(
    private val project: Project,
) {
    fun baseInformation(): BaseModel = baseModel {
        id = KotlinImportModelIds.BASE
        pluginVersion = project.kotlinToolingVersion.toImportModelVersion()
        project.kotlinJvmExtensionOrNull?.let { capabilities += BaseModel.Capability.CAPABILITY_KOTLIN_JVM }
            ?: project.multiplatformExtensionOrNull?.let {
                capabilities += BaseModel.Capability.CAPABILITY_KOTLIN_MULTIPLATFORM
            }
    }

    fun projectInformation(): ProjectModel = projectModel {
        id = KotlinImportModelIds.PROJECT_INFORMATION
        compilationUnitIds += supportedCompilations().map(::compilationUnitId)
    }

    fun compilationUnit(id: CompilationUnitId): CompilationUnitModel {
        val descriptor = compilation(id)
        return compilationUnitModel {
            this.id = KotlinImportModelIds.COMPILATION_UNIT
            parameters = CompilationUnitModelKt.parameters { compilationUnitId = id }
            name = descriptor.compilation.name
            platform = descriptor.platform
            purpose = descriptor.purpose
            targetPlatforms += descriptor.targetPlatforms
            descriptor.targetName?.let { targetName = it }
            sourceRoots += sourceRoots(descriptor.compilation)
            outputs += descriptor.readOutputs()
        }
    }

    fun compilationUnitDumpFileName(id: CompilationUnitId): String = compilation(id).let { descriptor ->
        "${descriptor.targetKey}-${descriptor.compilation.name}"
    }

    fun compilerArguments(id: CompilationUnitId): CompilerArgumentsModel {
        val compilation = compilation(id).compilation
        return compilerArgumentsModel {
            this.id = KotlinImportModelIds.COMPILER_ARGUMENTS
            parameters = CompilerArgumentsModelKt.parameters { compilationUnitId = id }
            arguments += IdeCompilerArgumentsResolver.instance(project)
                .resolveCompilerArguments(compilation)
                .orEmpty()
                .filterNot { it.startsWith("-Xplugin=") }
        }
    }

    fun dependencies(parameters: DependenciesModel.Parameters): DependenciesModel {
        val descriptor = compilation(parameters.compilationUnitId)
        val compilation = descriptor.compilation
        val configuration = when (parameters.scope) {
            DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE ->
                compilation.internal.configurations.compileDependencyConfiguration
            DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILER_PLUGIN ->
                compilation.internal.configurations.pluginConfiguration
            else -> error("Unsupported dependency scope")
        }
        val artifacts = configuration.lenientArtifactsView
        if (parameters.scope == DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILER_PLUGIN) {
            val pluginPaths = IdeCompilerArgumentsResolver.instance(project)
                .resolveCompilerArguments(compilation)
                .orEmpty()
                .mapNotNull { argument -> argument.removePrefix("-Xplugin=").takeIf { it != argument } }
            check(pluginPaths == artifacts.artifactFiles.files.map(File::getAbsolutePath)) {
                "Compiler plugin classpath does not match resolved plugin artifacts"
            }
        }

        return dependenciesModel {
            id = KotlinImportModelIds.DEPENDENCIES
            this.parameters = parameters
            classpathEntries += artifacts.artifactFiles.files.map { file ->
                artifacts.artifacts.single { it.file == file }.toClasspathEntry()
            }
            unresolvedDependencies += artifacts.failures.mapNotNull(::toUnresolvedDependency)
            if (parameters.scope == DependenciesModel.Scope.DEPENDENCY_SCOPE_COMPILE) {
                compilationRelations += compilationRelations(descriptor)
            }
        }
    }

    private fun compilation(id: CompilationUnitId): ImportCompilationDescriptor =
        supportedCompilations().singleOrNull { compilationUnitId(it) == id }
            ?: error("Unknown Kotlin import compilation unit '${id.value}' for project '${project.path}'")

    private fun supportedCompilations(): List<ImportCompilationDescriptor> = (
            project.kotlinJvmExtensionOrNull?.let { jvmProjectCompilations(it.target) }
                ?: project.multiplatformExtensionOrNull?.let(::multiplatformProjectCompilations)
                ?: emptyList()
            ).sortedBy { compilationUnitId(it).value }

    private fun jvmProjectCompilations(target: KotlinTarget): List<ImportCompilationDescriptor> {
        val targetName = target.targetName.ifEmpty { "jvm" }
        return importModelCompilationNames.map { name ->
            jvmCompilationDescriptor(target.compilations.getByName(name), targetName)
        }
    }

    private fun multiplatformProjectCompilations(kotlin: KotlinMultiplatformExtension): List<ImportCompilationDescriptor> {
        val metadataTarget = kotlin.metadata()
        val leafCompilations = kotlin.targets.withType(KotlinJvmTarget::class.java)
            .sortedBy { it.targetName }
            .flatMap { target ->
                importModelCompilationNames.map { name ->
                    jvmCompilationDescriptor(target.compilations.getByName(name), target.targetName)
                }
            } + kotlin.targets.withType(KotlinNativeTarget::class.java)
            .sortedBy { it.targetName }
            .flatMap { target ->
                importModelCompilationNames.map { name ->
                    nativeCompilationDescriptor(target.compilations.getByName(name), target.targetName)
                }
            }
        val metadataTargetPlatforms = leafCompilations
            .flatMap(ImportCompilationDescriptor::targetPlatforms)
            .distinct()
            .sortedBy(CompilationUnitModel.TargetPlatform::getNumber)
        return listOfNotNull(
            (metadataTarget.compilations.findByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) as? KotlinCommonCompilation)?.let { commonMain ->
                metadataCompilationDescriptor(commonMain, metadataTarget.name, metadataTargetPlatforms)
            },
        ) + leafCompilations
    }

    private fun jvmCompilationDescriptor(compilation: KotlinCompilation<*>, targetName: String): ImportCompilationDescriptor =
        ImportCompilationDescriptor(
            compilation = compilation,
            targetKey = targetName,
            platform = CompilationUnitModel.Platform.PLATFORM_JVM,
            targetPlatforms = listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_JVM),
            targetName = targetName,
            purpose = compilation.importModelPurpose(),
            readOutputs = { compilationOutputs(compilation.compileTaskProvider.get() as KotlinCompile) },
        )

    private fun nativeCompilationDescriptor(
        compilation: KotlinNativeCompilation,
        targetName: String,
    ): ImportCompilationDescriptor = ImportCompilationDescriptor(
        compilation = compilation,
        targetKey = targetName,
        platform = CompilationUnitModel.Platform.PLATFORM_NATIVE,
        targetPlatforms = listOf(CompilationUnitModel.TargetPlatform.TARGET_PLATFORM_NATIVE),
        targetName = targetName,
        purpose = compilation.importModelPurpose(),
        readOutputs = { compilationOutputs(compilation.compileTaskProvider.get()) },
    )

    private fun metadataCompilationDescriptor(
        compilation: KotlinCommonCompilation,
        targetKey: String,
        targetPlatforms: List<CompilationUnitModel.TargetPlatform>,
    ): ImportCompilationDescriptor = ImportCompilationDescriptor(
        compilation = compilation,
        targetKey = targetKey,
        platform = CompilationUnitModel.Platform.PLATFORM_METADATA,
        targetPlatforms = targetPlatforms,
        targetName = null,
        purpose = CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN,
        readOutputs = { compilationOutputs(compilation.compileTaskProvider.get() as KotlinCompileCommon) },
    )

    private fun compilationRelations(compilation: ImportCompilationDescriptor): List<DependenciesModel.CompilationRelation> {
        val supportedCompilations = supportedCompilations()
        return compilation.compilation.associatedCompilations
            .mapNotNull { associatedCompilation -> supportedCompilations.singleOrNull { it.compilation == associatedCompilation } }
            .sortedBy { compilationUnitId(it).value }
            .map { associatedCompilation ->
                DependenciesModelKt.compilationRelation {
                    kind = DependenciesModel.CompilationRelation.Kind.COMPILATION_RELATION_KIND_FRIEND
                    targetCompilationUnitId = compilationUnitId(associatedCompilation)
                }
            }
    }

    private fun compilationOutputs(compileTask: KotlinCompile): List<CompilationUnitModel.Output> {
        val producingAction = gradleAction(compileTask.path)
        val outputs = buildList {
            add(
                compileTask.destinationDirectory.get().asFile.toPath() to
                        CompilationUnitModel.Output.Kind.OUTPUT_KIND_CLASSES
            )
            if (compileTask.runViaBuildToolsApi.getOrElse(false) && compileTask.generateCompilerRefIndex.getOrElse(false)) {
                add(
                    compileTask.taskBuildCacheableOutputDirectory.get().dir(DATA_PATH).asFile.toPath() to
                            CompilationUnitModel.Output.Kind.OUTPUT_KIND_CRI
                )
            }
        }
        return outputs
            .distinctBy { (path, _) -> path }
            .map { (path, kind) ->
                CompilationUnitModelKt.output {
                    this.path = project.relativeProjectPath(path)
                    this.kind = kind
                    producingActions += producingAction
                }
            }
            .sortedBy(CompilationUnitModel.Output::getPath)
    }

    private fun compilationOutputs(compileTask: KotlinNativeCompile): List<CompilationUnitModel.Output> = compilationOutputs(
        compileTask.path,
        listOf(compileTask.outputFile.get().toPath() to CompilationUnitModel.Output.Kind.OUTPUT_KIND_KLIB),
    )

    private fun compilationOutputs(compileTask: KotlinCompileCommon): List<CompilationUnitModel.Output> = compilationOutputs(
        compileTask.path,
        listOf(compileTask.destinationDirectory.get().asFile.toPath() to CompilationUnitModel.Output.Kind.OUTPUT_KIND_KLIB),
    )

    private fun compilationOutputs(
        compileTaskPath: String,
        outputs: List<Pair<Path, CompilationUnitModel.Output.Kind>>,
    ): List<CompilationUnitModel.Output> = outputs
        .distinctBy { (path, _) -> path }
        .map { (path, kind) ->
            CompilationUnitModelKt.output {
                this.path = project.relativeProjectPath(path)
                this.kind = kind
                producingActions += gradleAction(compileTaskPath)
            }
        }
        .sortedBy(CompilationUnitModel.Output::getPath)

    private fun KotlinCompilation<*>.importModelPurpose(): CompilationUnitModel.Purpose = when {
        isMain() -> CompilationUnitModel.Purpose.COMPILATION_PURPOSE_MAIN
        isTest() -> CompilationUnitModel.Purpose.COMPILATION_PURPOSE_TEST
        else -> error("Unsupported Kotlin import compilation purpose '$name' for project '${project.path}'")
    }

    private fun ResolvedArtifactResult.toClasspathEntry(): DependenciesModel.ClasspathEntry =
        DependenciesModelKt.classpathEntry {
            when (val component = id.componentIdentifier) {
                is ProjectComponentIdentifier -> project = DependenciesModelKt.projectDependency {
                    buildPath = component.build.compatAccessor(this@KotlinImportModelProvider.project).buildPath
                    projectPath = component.projectPath
                    artifactPath = file.absolutePath
                    variant.attributes.getAttribute(kotlinImportModelCompilationIdAttribute)
                        ?.takeIf(String::isNotEmpty)
                        ?.let { targetCompilationUnitId = compilationUnitId { value = it } }
                }
                else -> binary = toBinaryDependency()
            }
        }

    private fun ResolvedArtifactResult.toBinaryDependency(): BinaryDependency = when (val component = id.componentIdentifier) {
        is ModuleComponentIdentifier -> binaryDependency {
            coordinates = mavenCoordinates {
                group = component.group
                module = component.module
                version = component.version
            }
            artifactPath = file.absolutePath
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

        return compilation.allKotlinSourceSets.flatMap { sourceSet ->
            val generatedKotlin = sourceSet.generatedKotlin
            val generatedRoots = generatedKotlin.srcDirs.map(File::toPath)
            val generatedRootActions = generatedKotlin.buildDependencies.getDependenciesForInternalUse()
                .sortedBy { it.path }
                .map { producer -> gradleAction(producer.path) }
            roots(SourceRoot.Kind.SOURCE_ROOT_KIND_SOURCE, sourceSet.kotlin.srcDirs.map(File::toPath)) +
                    roots(SourceRoot.Kind.SOURCE_ROOT_KIND_GENERATED, generatedRoots) {
                        generatedRootActions
                    }
        }
            .sortedWith(compareBy(SourceRoot::getPath).thenBy(SourceRoot::getKindValue))
            .distinctBy(SourceRoot::getPath)
    }

    private fun gradleAction(taskPath: String): Action = action {
        gradleAction = ActionKt.gradleTask { this.taskPath = taskPath }
    }

    // Not using TaskDependency.getDependencies() because it can trigger configuration-cache warnings
    private fun TaskDependency.getDependenciesForInternalUse(): Set<Task> {
        val getDependenciesForInternalUse = try {
            Class.forName("org.gradle.api.internal.tasks.TaskDependencyUtil")
                .getMethod("getDependenciesForInternalUse", TaskDependency::class.java, Task::class.java)
        } catch (_: ClassNotFoundException) {
            return getDependencies(null)
        } catch (_: NoSuchMethodException) {
            return getDependencies(null)
        }
        @Suppress("UNCHECKED_CAST")
        return try {
            getDependenciesForInternalUse.invoke(null, this, null) as Set<Task>
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private fun Project.relativeProjectPath(path: Path): String =
        Paths.get(relativePath(path.toFile())).invariantSeparatorsPathString

    private fun compilationUnitId(compilation: ImportCompilationDescriptor): CompilationUnitId {
        val buildPath = project.currentBuildId().compatAccessor(project).buildPath
        return compilationUnitId {
            value = compilationUnitIdValue(buildPath, project.path, compilation.targetKey, compilation.compilation.name)
        }
    }

    private data class ImportCompilationDescriptor(
        val compilation: KotlinCompilation<*>,
        val targetKey: String,
        val platform: CompilationUnitModel.Platform,
        val targetPlatforms: List<CompilationUnitModel.TargetPlatform>,
        val targetName: String?,
        val purpose: CompilationUnitModel.Purpose,
        val readOutputs: () -> List<CompilationUnitModel.Output>,
    )
}

private val importModelCompilationNames = listOf(
    KotlinCompilation.MAIN_COMPILATION_NAME,
    KotlinCompilation.TEST_COMPILATION_NAME,
)

// Stable opaque format: percent-escape each component before joining with `|`
internal fun compilationUnitIdValue(
    buildPath: String,
    projectPath: String,
    targetKey: String,
    compilationName: String,
): String = listOf(buildPath, projectPath, targetKey, compilationName)
    .joinToString("|") { it.replace("%", "%25").replace("|", "%7C") }

private fun KotlinToolingVersion.toImportModelVersion(): Version = version {
    major = this@toImportModelVersion.major
    minor = this@toImportModelVersion.minor
    patch = this@toImportModelVersion.patch
    this@toImportModelVersion.classifier?.let { classifier = it }
}
