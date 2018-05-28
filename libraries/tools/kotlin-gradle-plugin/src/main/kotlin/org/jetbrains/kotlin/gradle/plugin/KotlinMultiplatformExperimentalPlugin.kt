/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinOnlyPlatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinPlatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.base.KotlinOnlyPlatformConfigurator
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinOnlySourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinOnlySourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCommonTasksProvider
import org.jetbrains.kotlin.gradle.utils.matchSymmetricallyByNames

private val Project.multiplatformExtension get() = kotlinExtension as KotlinMultiplatformExtension

internal class KotlinMultiplatformProjectConfigurator(
    private val project: Project,
    private val objectFactory: ObjectFactory,
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator,
    private val kotlinGradleBuildServices: KotlinGradleBuildServices,
    private val kotlinPluginVersion: String,
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) {
    val kotlinOnlyPlatformConfigurator = KotlinOnlyPlatformConfigurator(buildOutputCleanupRegistry, objectFactory)

    private inline fun <reified T : KotlinOnlyPlatformExtension> createKotlinOnlyExtension(
        name: String,
        platformClassifier: String,
        sourceSetContainer: KotlinSourceSetContainer<*>
    ): T {
        val extension = objectFactory.newInstance(T::class.java).apply {
            platformName = name
            platformDisambiguationClassifier = platformClassifier
        }
        val multiplatformExtension = project.multiplatformExtension
        (multiplatformExtension as ExtensionAware).extensions.add(platformClassifier, extension)
        registerKotlinSourceSetsIfAbsent(sourceSetContainer, extension)
        kotlinOnlyPlatformConfigurator.configureKotlinPlatform(project, extension)
        return extension
    }

    fun createJvmExtension(disambiguationSuffix: String? = null): KotlinOnlyPlatformExtension {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val platformSuffix = disambiguationSuffix?.capitalize().orEmpty()
        val platformName = "kotlinJvm$platformSuffix"
        val platformClassifier = "jvm$platformSuffix"
        val extension = createKotlinOnlyExtension<KotlinOnlyPlatformExtension>(platformName, platformClassifier, sourceSets)

        val tasksProvider = KotlinCommonTasksProvider()
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            Kotlin2JvmSourceSetProcessor(project, sourceSet, tasksProvider, sourceSets, kotlinPluginVersion, kotlinGradleBuildServices)
        }

        return extension
    }

    fun createCommonExtension(): KotlinOnlyPlatformExtension {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val extension = createKotlinOnlyExtension<KotlinOnlyPlatformExtension>("kotlinCommon", "common", sourceSets)

        val tasksProvider = KotlinCommonTasksProvider()
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            KotlinCommonSourceSetProcessor(project, sourceSet, tasksProvider, sourceSets)
        }

        return extension
    }

    fun createJsPlatformExtension(disambiguationSuffix: String? = null): KotlinOnlyPlatformExtension {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val platformSuffix = disambiguationSuffix?.capitalize().orEmpty()
        val platformName = "kotlinJs$platformSuffix"
        val platformClassifier = "js$platformSuffix"
        val extension = createKotlinOnlyExtension<KotlinOnlyPlatformExtension>(platformName, platformClassifier, sourceSets)

        val tasksProvider = Kotlin2JsTasksProvider()
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            Kotlin2JsSourceSetProcessor(project, sourceSet, tasksProvider, sourceSets)
        }

        project.multiplatformExtension.common {
            linkCommonAndPlatformExtensions(this@common, extension)
        }

        return extension
    }

    private inline fun <reified T : KotlinSourceSet> configureSourceSetDefaults(
        extension: KotlinPlatformExtension,
        crossinline buildSourceSetProcessor: (T) -> KotlinSourceSetProcessor<*>
    ) {
        extension.sourceSets.all { sourceSet ->
            sourceSet as T
            buildSourceSetProcessor(sourceSet).run()
        }
    }

    protected fun linkCommonAndPlatformExtensions(
        commonExtension: KotlinPlatformExtension,
        platformExtension: KotlinPlatformExtension
    ) {
        matchSymmetricallyByNames(commonExtension.sourceSets, platformExtension.sourceSets) { commonSourceSet: KotlinSourceSet, _ ->
            val platformTask = project.tasks
                .filterIsInstance<AbstractKotlinCompile<*>>()
                .firstOrNull { it.sourceSetName == commonSourceSet.name }

            platformTask?.source(commonSourceSet.kotlin)
        }
    }
}

internal class KotlinMultiplatformPlugin(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val objectFactory: ObjectFactory,
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator,
    private val kotlinGradleBuildServices: KotlinGradleBuildServices,
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)
        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        kotlinMultiplatformExtension.projectConfigurator = KotlinMultiplatformProjectConfigurator(
            project, objectFactory, fileResolver, instantiator,
            kotlinGradleBuildServices, kotlinPluginVersion, buildOutputCleanupRegistry
        )

        kotlinMultiplatformExtension.common { } // make it configure by default
    }
}