/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package org.jetbrains.kotlin.gradle.tasks.configuration

import com.intellij.util.lang.JavaVersion
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.topLevelExtension
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.classLoadersCacheSize
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.disableClassloaderCacheForProcessors
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isIncludeCompileClasspath
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.isIncrementalKapt
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.CLASS_STRUCTURE_ARTIFACT_TYPE
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformAction
import org.jetbrains.kotlin.gradle.internal.kapt.incremental.StructureTransformLegacyAction
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.toCompilerPluginOptions
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.detachedResolvable
import org.jetbrains.kotlin.gradle.utils.listProperty
import java.io.File

internal open class KaptConfig<TASK : KaptTask>(
    project: Project,
    protected val ext: KaptExtension,
) : TaskConfigAction<TASK>(project) {

    init {
        configureTaskProvider { taskProvider ->
            val kaptClasspathSnapshot = getKaptClasspathSnapshot(taskProvider)

            taskProvider.configure { task ->
                task.verbose.set(KaptTask.queryKaptVerboseProperty(project))

                task.isIncremental = project.isIncrementalKapt()
                task.useBuildCache = ext.useBuildCache

                task.includeCompileClasspath.set(ext.includeCompileClasspath ?: project.isIncludeCompileClasspath())
                task.classpathStructure.from(kaptClasspathSnapshot)

                task.localStateDirectories.from({ task.incAptCache.orNull })
                task.onlyIf {
                    it as KaptTask
                    it.includeCompileClasspath.get() || !it.kaptClasspath.isEmpty
                }
            }
        }
    }

    internal constructor(
        project: Project,
        kaptGenerateStubsTask: TaskProvider<KaptGenerateStubsTask>,
        ext: KaptExtension
    ) : this(project, ext) {
        configureTask { task ->
            task.classpath.from(
                kaptGenerateStubsTask.map { it.libraries }
            )
            task.compiledSources
                .from(
                    kaptGenerateStubsTask.flatMap { it.kotlinCompileDestinationDirectory },
                    { kaptGenerateStubsTask.get().javaOutputDir.takeIf { it.isPresent } }
                )
                .disallowChanges()
            task.sourceSetName.value(kaptGenerateStubsTask.flatMap { it.sourceSetName }).disallowChanges()


            val kaptSources = objectFactory.fileCollection()
                .from(kaptGenerateStubsTask.map { it.javaSources }, task.stubsDir)
                .asFileTree
                .matching { it.include("**/*.java") }
                .filter {
                    it.exists() &&
                            !isAncestor(task.destinationDir.get().asFile, it) &&
                            !isAncestor(task.classesDir.get().asFile, it)
                }
            task.source.from(kaptSources).disallowChanges()
        }
    }

    private fun getKaptClasspathSnapshot(taskProvider: TaskProvider<TASK>): FileCollection? {
        return if (project.isIncrementalKapt()) {
            maybeRegisterTransform(project)

            val classStructureConfiguration = project.configurations.detachedResolvable()

            // Wrap the `kotlinCompile.classpath` into a file collection, so that, if the classpath is represented by a configuration,
            // the configuration is not extended (via extendsFrom, which normally happens when one configuration is _added_ into another)
            // but is instead included as the (lazily) resolved files. This is needed because the class structure configuration doesn't have
            // the attributes that are potentially needed to resolve dependencies on MPP modules, and the classpath configuration does.
            classStructureConfiguration.dependencies.add(project.dependencies.create(project.files(project.provider { taskProvider.get().classpath })))
            classStructureConfiguration.incoming.artifactView { viewConfig ->
                viewConfig.attributes.setAttribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }.files
        } else null
    }

    private fun maybeRegisterTransform(project: Project) {
        if (!project.extensions.extraProperties.has("KaptStructureTransformAdded")) {
            val transformActionClass =
                if (GradleVersion.current() >= GradleVersion.version("5.4"))
                    StructureTransformAction::class.java
                else
                    StructureTransformLegacyAction::class.java
            project.dependencies.registerTransform(transformActionClass) { transformSpec ->
                transformSpec.from.setAttribute(artifactType, "jar")
                transformSpec.to.setAttribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }

            project.dependencies.registerTransform(transformActionClass) { transformSpec ->
                transformSpec.from.setAttribute(artifactType, "directory")
                transformSpec.to.setAttribute(artifactType, CLASS_STRUCTURE_ARTIFACT_TYPE)
            }

            project.extensions.extraProperties["KaptStructureTransformAdded"] = true
        }
    }

    internal fun getJavaOptions(defaultJavaSourceCompatibility: Provider<String>): Provider<Map<String, String>> {
        return providers.provider {
            ext.getJavacOptions().toMutableMap().also { result ->
                if ("-source" in result || "--source" in result || "--release" in result) return@also

                if (defaultJavaSourceCompatibility.isPresent) {
                    val atLeast12Java = JavaVersion.current().compareTo(JavaVersion.compose(12, 0, 0, 0, false)) >= 0
                    val sourceOptionKey = if (atLeast12Java) {
                        "--source"
                    } else {
                        "-source"
                    }
                    result[sourceOptionKey] = defaultJavaSourceCompatibility.get()
                }
            }
        }
    }
}

//Have to avoid using FileUtil because it is required system property reading that is not allowed for configuration cache
private fun isAncestor(dir: File, file: File): Boolean {
    val path = file.normalize().absolutePath
    val prefix = dir.normalize().absolutePath
    val pathLength = path.length
    val prefixLength = prefix.length
    val caseSensitive = true
    return if (prefixLength == 0) {
        true
    } else if (prefixLength > pathLength) {
        false
    } else if (!path.regionMatches(0, prefix, 0, prefixLength, ignoreCase = !caseSensitive)) {
        return false
    } else if (pathLength == prefixLength) {
        return true
    } else {
        val lastPrefixChar: Char = prefix.get(prefixLength - 1)
        var slashOrSeparatorIdx = prefixLength
        if (lastPrefixChar == '/' || lastPrefixChar == File.separatorChar) {
            slashOrSeparatorIdx = prefixLength - 1
        }
        val next1 = path[slashOrSeparatorIdx]
        return !(next1 != '/' && next1 != File.separatorChar)
    }
}

internal class KaptWithoutKotlincConfig : KaptConfig<KaptWithoutKotlincTask> {

    init {
        configureTask { task ->
            task.addJdkClassesToClasspath.set(
                project.providers.provider {
                    project.plugins.none { it is AbstractKotlinAndroidPluginWrapper }
                }
            )
            task.kaptJars.from(project.configurations.getByName(Kapt3GradleSubplugin.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME))
            task.mapDiagnosticLocations = ext.mapDiagnosticLocations
            task.annotationProcessorFqNames.set(providers.provider {
                @Suppress("DEPRECATION")
                ext.processors.split(',').filter { it.isNotEmpty() }
            })
            task.disableClassloaderCacheForProcessors = project.disableClassloaderCacheForProcessors()
            task.classLoadersCacheSize = project.classLoadersCacheSize()
            task.javacOptions.set(getJavaOptions(task.defaultJavaSourceCompatibility))
        }
    }

    constructor(
        project: Project,
        kaptGenerateStubsTask: TaskProvider<KaptGenerateStubsTask>,
        ext: KaptExtension
    ) : super(project, kaptGenerateStubsTask, ext) {
        project.configurations.findResolvable(Kapt3GradleSubplugin.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME)
            ?: project.configurations.createResolvable(Kapt3GradleSubplugin.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME).apply {
                dependencies.addAllLater(project.listProperty {
                    val kaptDependency = "org.jetbrains.kotlin:kotlin-annotation-processing-gradle:${project.getKotlinPluginVersion()}"
                    listOf(
                        project.dependencies.create(kaptDependency),
                        project.dependencies.kotlinDependency(
                            "kotlin-stdlib",
                            project.topLevelExtension.coreLibrariesVersion
                        )
                    )
                })
            }

        configureTask { task ->
            task.addJdkClassesToClasspath.set(
                project.providers.provider {
                    project.plugins.none { it is AbstractKotlinAndroidPluginWrapper }
                }
            )
            task.kaptJars.from(project.configurations.getByName(Kapt3GradleSubplugin.KAPT_WORKER_DEPENDENCIES_CONFIGURATION_NAME))
        }
    }

    constructor(project: Project, ext: KaptExtension) : super(project, ext) {
        configureTask { task ->
            val kotlinSourceDir = objectFactory.fileCollection().from(task.kotlinSourcesDestinationDir)
            val nonAndroidDslOptions = getNonAndroidDslApOptions(ext, project, kotlinSourceDir, null, null)
            task.kaptPluginOptions.add(nonAndroidDslOptions.toCompilerPluginOptions())
        }
    }
}

private val artifactType = Attribute.of("artifactType", String::class.java)