/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.incremental.IncrementalModuleInfoBuildService
import org.jetbrains.kotlin.gradle.internal.ClassLoadersCachingBuildService
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.transforms.BuildToolsApiClasspathEntrySnapshotTransform
import org.jetbrains.kotlin.gradle.plugin.BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupTransformActionToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.plugin.tcs
import org.jetbrains.kotlin.gradle.tasks.DefaultKotlinJavaToolchain
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.detachedResolvable
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import org.jetbrains.kotlin.gradle.utils.setAttribute

internal typealias KotlinCompileConfig = BaseKotlinCompileConfig<KotlinCompile>

internal open class BaseKotlinCompileConfig<TASK : KotlinCompile> : AbstractKotlinCompileConfig<TASK> {

    init {
        configureTaskProvider { taskProvider ->
            val useClasspathSnapshot = propertiesProvider.useClasspathSnapshot

            val classpathConfiguration = if (useClasspathSnapshot) {
                val jvmToolchain = taskProvider.flatMap { it.defaultKotlinJavaToolchain }
                val runKotlinCompilerViaBuildToolsApi = propertiesProvider.runKotlinCompilerViaBuildToolsApi
                registerTransformsOnce(project, jvmToolchain, runKotlinCompilerViaBuildToolsApi)
                // Note: Creating configurations should be done during build configuration, not task configuration, to avoid issues with
                // composite builds (e.g., https://issuetracker.google.com/183952598).
                project.configurations.detachedResolvable(
                    project.dependencies.create(objectFactory.fileCollection().from(project.provider { taskProvider.get().libraries }))
                )
            } else null

            if (useClasspathSnapshot) {
                taskProvider.configure { it.incrementalModuleInfoProvider.disallowChanges() }
            } else {
                val incrementalModuleInfoProvider = IncrementalModuleInfoBuildService.registerIfAbsent(
                    project,
                    objectFactory.providerWithLazyConvention { GradleCompilerRunner.buildModulesInfo(project.gradle) },
                )
                taskProvider.configure { it.incrementalModuleInfoProvider.value(incrementalModuleInfoProvider).disallowChanges() }
            }

            taskProvider.configure { task ->
                task.incremental = propertiesProvider.incrementalJvm ?: true
                task.usePreciseJavaTracking = propertiesProvider.usePreciseJavaTracking ?: true
                task.jvmTargetValidationMode.convention(propertiesProvider.jvmTargetValidationMode).finalizeValueOnRead()
                task.useKotlinAbiSnapshot.value(propertiesProvider.useKotlinAbiSnapshot).disallowChanges()

                task.classpathSnapshotProperties.useClasspathSnapshot.value(useClasspathSnapshot).disallowChanges()
                if (useClasspathSnapshot) {
                    val classpathEntrySnapshotFiles = classpathConfiguration!!.incoming.artifactView {
                        it.attributes.setAttribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
                    }.files
                    task.classpathSnapshotProperties.classpathSnapshot.from(classpathEntrySnapshotFiles).disallowChanges()
                    task.classpathSnapshotProperties.classpathSnapshotDir.value(getClasspathSnapshotDir(task)).disallowChanges()
                } else {
                    task.classpathSnapshotProperties.classpath.from(task.project.provider { task.libraries }).disallowChanges()
                }
                task.taskOutputsBackupExcludes.addAll(
                    task.classpathSnapshotProperties.classpathSnapshotDir.asFile.flatMap {
                        // it looks weird, but it's required to work around this issue: https://github.com/gradle/gradle/issues/17704
                        objectFactory.providerWithLazyConvention { listOf(it) }
                    }.orElse(emptyList())
                )
            }
        }
    }

    constructor(compilationInfo: KotlinCompilationInfo) : super(compilationInfo) {
        val javaTaskProvider = when (val compilation = compilationInfo.tcs.compilation) {
            is KotlinJvmCompilation -> compilation.compileJavaTaskProviderSafe
            is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
            is KotlinWithJavaCompilation<*, *> -> compilation.compileJavaTaskProvider
            else -> null
        }

        configureTaskProvider { taskProvider ->
            taskProvider.configure { task ->
                javaTaskProvider?.let { javaTask ->
                    task.associatedJavaCompileTaskTargetCompatibility.value(javaTask.map { it.targetCompatibility })
                    task.associatedJavaCompileTaskName.value(javaTask.map { it.name })
                }

                task.nagTaskModuleNameUsage.value(true).disallowChanges()
            }
        }
    }


    constructor(project: Project, ext: KotlinTopLevelExtension) : super(
        project, ext
    )

    companion object {
        private const val TRANSFORMS_REGISTERED = "_kgp_internal_kotlin_compile_transforms_registered"

        val ARTIFACT_TYPE_ATTRIBUTE: Attribute<String> = Attribute.of("artifactType", String::class.java)
        private const val DIRECTORY_ARTIFACT_TYPE = "directory"
        private const val JAR_ARTIFACT_TYPE = "jar"
        const val CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE = "classpath-entry-snapshot"
    }

    private fun registerTransformsOnce(
        project: Project,
        jvmToolchain: Provider<DefaultKotlinJavaToolchain>,
        runKotlinCompilerViaBuildToolsApi: Boolean,
    ) {
        if (project.extensions.extraProperties.has(TRANSFORMS_REGISTERED)) {
            return
        }
        project.extensions.extraProperties[TRANSFORMS_REGISTERED] = true

        registerBuildToolsApiTransformations(project, jvmToolchain, runKotlinCompilerViaBuildToolsApi)
    }

    private fun TransformSpec<BuildToolsApiClasspathEntrySnapshotTransform.Parameters>.configureCommonParameters(
        kgpVersion: String,
        classLoadersCachingService: Provider<ClassLoadersCachingBuildService>,
        classpath: Provider<out Configuration>,
        jvmToolchain: Provider<DefaultKotlinJavaToolchain>,
        runKotlinCompilerViaBuildToolsApi: Boolean,
        suppressVersionInconsistencyChecks: Boolean,
    ) {
        parameters.gradleUserHomeDir.set(project.gradle.gradleUserHomeDir)
        parameters.classLoadersCachingService.set(classLoadersCachingService)
        parameters.classpath.from(classpath)
        // add some tools.jar in order to reuse some of the classloaders required for compilation
        parameters.classpath.from(jvmToolchain.map { toolchain ->
            if (toolchain.currentJvmJdkToolsJar.isPresent) {
                setOf(toolchain.currentJvmJdkToolsJar.get())
            } else {
                emptySet()
            }
        })
        parameters.compilationViaBuildToolsApi.set(runKotlinCompilerViaBuildToolsApi)
        if (!suppressVersionInconsistencyChecks) {
            parameters.buildToolsImplVersion.set(classpath.map { configuration -> configuration.findBuildToolsApiImplVersion() })
        }
        parameters.kgpVersion.set(kgpVersion)
        parameters.suppressVersionInconsistencyChecks.set(suppressVersionInconsistencyChecks)
    }

    private fun Configuration.findBuildToolsApiImplVersion() = incoming.resolutionResult.allDependencies
        .filterIsInstance<ResolvedDependencyResult>()
        .map { it.selected.id }
        .filterIsInstance<ModuleComponentIdentifier>()
        .find { it.group == KOTLIN_MODULE_GROUP && it.module == KOTLIN_BUILD_TOOLS_API_IMPL }
        ?.version ?: "null" // workaround for incorrect nullability of `map`

    private fun registerBuildToolsApiTransformations(
        project: Project,
        jvmToolchain: Provider<DefaultKotlinJavaToolchain>,
        runKotlinCompilerViaBuildToolsApi: Boolean
    ) {
        val classLoadersCachingService = ClassLoadersCachingBuildService.registerIfAbsent(project)
        val classpath = project.configurations.named(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
        val kgpVersion = project.getKotlinPluginVersion()
        val suppressVersionInconsistencyChecks = project.kotlinPropertiesProvider.suppressBuildToolsApiVersionConsistencyChecks
        project.dependencies.registerTransform(BuildToolsApiClasspathEntrySnapshotTransform::class.java) {
            it.from.setAttribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_ARTIFACT_TYPE)
            it.to.setAttribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
            it.configureCommonParameters(kgpVersion, classLoadersCachingService, classpath, jvmToolchain, runKotlinCompilerViaBuildToolsApi, suppressVersionInconsistencyChecks)
            it.setupTransformActionToolingDiagnostics(project)
        }
        project.dependencies.registerTransform(BuildToolsApiClasspathEntrySnapshotTransform::class.java) {
            it.from.setAttribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_ARTIFACT_TYPE)
            it.to.setAttribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
            it.configureCommonParameters(kgpVersion, classLoadersCachingService, classpath, jvmToolchain, runKotlinCompilerViaBuildToolsApi, suppressVersionInconsistencyChecks)
            it.setupTransformActionToolingDiagnostics(project)
        }
    }

}