/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.configuration

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension
import org.jetbrains.kotlin.gradle.internal.transforms.ClasspathEntrySnapshotTransform
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.report.BuildMetricsReporterService
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.project.model.LanguageSettings

internal typealias KotlinCompileConfig = BaseKotlinCompileConfig<KotlinCompile>

internal open class BaseKotlinCompileConfig<TASK : KotlinCompile> : AbstractKotlinCompileConfig<TASK> {

    init {
        configureTaskProvider { taskProvider ->
            val snapshotConfiguration = runAtConfigurationTime(taskProvider)

            taskProvider.configure { task ->
                task.incremental = propertiesProvider.incrementalJvm ?: true

                if (propertiesProvider.useK2 == true) {
                    task.kotlinOptions.useK2 = true
                }
                task.usePreciseJavaTracking = propertiesProvider.usePreciseJavaTracking ?: true
                task.jvmTargetValidationMode.set(propertiesProvider.jvmTargetValidationMode)
                task.classpathSnapshotProperties.useClasspathSnapshot.value(propertiesProvider.useClasspathSnapshot)
                task.useKotlinAbiSnapshot.value(propertiesProvider.useKotlinAbiSnapshot).disallowChanges()

                if (snapshotConfiguration != null) {
                    val snapshotFiles = snapshotConfiguration.incoming.artifactView {
                        it.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
                    }.files
                    task.classpathSnapshotProperties.classpathSnapshot.from(snapshotFiles).disallowChanges()
                    task.classpathSnapshotProperties.classpathSnapshotDir
                        .value(getClasspathSnapshotDir(task))
                        .disallowChanges()
                } else {
                    task.classpathSnapshotProperties.classpath.from(task.project.provider { task.libraries })
                }
            }
        }
    }

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(compilation: KotlinCompilationData<*>) : super(compilation) {
        val javaTaskProvider = when (compilation) {
            is KotlinJvmCompilation -> compilation.compileJavaTaskProvider
            is KotlinJvmAndroidCompilation -> compilation.compileJavaTaskProvider
            is KotlinWithJavaCompilation<*> -> compilation.compileJavaTaskProvider
            else -> null
        }

        configureTaskProvider { taskProvider ->
            taskProvider.configure { task ->
                javaTaskProvider?.let {
                    task.associatedJavaCompileTaskTargetCompatibility.value(javaTaskProvider.map { it.targetCompatibility })
                    task.associatedJavaCompileTaskSources.from(javaTaskProvider.map { it.source })
                    task.associatedJavaCompileTaskName.value(javaTaskProvider.name)
                }
                task.moduleName.value(providers.provider {
                    (compilation.kotlinOptions as? KotlinJvmOptions)?.moduleName ?: task.parentKotlinOptions.orNull?.moduleName
                    ?: compilation.moduleName
                })
            }
        }
    }

    constructor(project: Project, ext: KotlinTopLevelExtension) : super(
        project, ext, languageSettings = getDefaultLangSetting(project, ext)
    )

    companion object {
        private const val TRANSFORMS_REGISTERED = "_kgp_internal_kotlin_compile_transforms_registered"

        val ARTIFACT_TYPE_ATTRIBUTE: Attribute<String> = Attribute.of("artifactType", String::class.java)
        private const val DIRECTORY_ARTIFACT_TYPE = "directory"
        private const val JAR_ARTIFACT_TYPE = "jar"
        const val CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE = "classpath-entry-snapshot"

        private fun getDefaultLangSetting(project: Project, ext: KotlinTopLevelExtension): Provider<LanguageSettings> {
            return project.provider {
                DefaultLanguageSettingsBuilder().also {
                    it.freeCompilerArgsProvider = project.provider { listOfNotNull(ext.explicitApi?.toCompilerArg()) }
                }
            }
        }
    }

    private fun registerTransformsOnce(project: Project) {
        if (project.extensions.extraProperties.has(TRANSFORMS_REGISTERED)) {
            return
        }
        project.extensions.extraProperties[TRANSFORMS_REGISTERED] = true

        val buildMetricsReporterService = BuildMetricsReporterService.registerIfAbsent(project)
        project.dependencies.registerTransform(ClasspathEntrySnapshotTransform::class.java) {
            it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, JAR_ARTIFACT_TYPE)
            it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
            it.parameters.gradleUserHomeDir.set(project.gradle.gradleUserHomeDir)
            buildMetricsReporterService?.apply { it.parameters.buildMetricsReporterService.set(this) }
        }
        project.dependencies.registerTransform(ClasspathEntrySnapshotTransform::class.java) {
            it.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_ARTIFACT_TYPE)
            it.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, CLASSPATH_ENTRY_SNAPSHOT_ARTIFACT_TYPE)
            it.parameters.gradleUserHomeDir.set(project.gradle.gradleUserHomeDir)
            buildMetricsReporterService?.apply { it.parameters.buildMetricsReporterService.set(this) }
        }
    }

    /**
     * Prepares for configuration of the task. This method must be called during build configuration, not during task configuration
     * (which typically happens after build configuration). The reason is that some actions must be performed early (e.g., creating
     * configurations should be done early to avoid issues with composite builds (https://issuetracker.google.com/183952598)).
     */
    private fun runAtConfigurationTime(taskProvider: TaskProvider<TASK>): Configuration? {
        return if (propertiesProvider.useClasspathSnapshot) {
            registerTransformsOnce(project)
            project.configurations.detachedConfiguration(
                project.dependencies.create(objectFactory.fileCollection().from(project.provider { taskProvider.get().libraries }))
            )
        } else null
    }
}