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
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCommonTasksProvider
import org.jetbrains.kotlin.gradle.utils.matchSymmetricallyByNames

class KotlinMultiplatformPlugin(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val objectFactory: ObjectFactory,
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator
) : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)

        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        kotlinMultiplatformExtension.project = project
        kotlinMultiplatformExtension.multiplatformPlugin = this

        kotlinMultiplatformExtension.common { } // make it configure by default
    }

    private val Project.multiplatformExtension get() = kotlinExtension as KotlinMultiplatformExtension

    // FIXME extract to a configurator class that would hold a Project
    fun createJsPlatformExtension(project: Project): KotlinOnlyPlatformExtension {
        val extension = objectFactory.newInstance(KotlinOnlyPlatformExtension::class.java).apply {
            platformName = "kotlin2Js"
            platformDisambiguationClassifier = "js"
        }
        val multiplatformExtension = project.multiplatformExtension
        (multiplatformExtension as ExtensionAware).extensions.add("js", extension)
        val sourceSetContainer = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val tasksProvider = Kotlin2JsTasksProvider()
        registerKotlinSourceSetsIfAbsent(sourceSetContainer, extension)
        KotlinOnlyPlatformConfigurator(buildOutputCleanupRegistry, objectFactory).configureKotlinPlatform(project, extension)
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            Kotlin2JsSourceSetProcessor(project, sourceSet, tasksProvider, sourceSetContainer)
        }

        multiplatformExtension.common {
            linkCommonAndPlatformExtensions(project, this@common, extension)
        }

        return extension
    }

    fun createCommonExtension(project: Project): KotlinOnlyPlatformExtension {
        val extension = objectFactory.newInstance(KotlinOnlyPlatformExtension::class.java).apply {
            platformName = "kotlinCommon"
            platformDisambiguationClassifier = "common"
        }
        val multiplatformExtension = project.multiplatformExtension
        (multiplatformExtension as ExtensionAware).extensions.add("common", extension)
        val sourceSetContainer = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val tasksProvider = KotlinCommonTasksProvider()
        registerKotlinSourceSetsIfAbsent(sourceSetContainer, extension)
        KotlinOnlyPlatformConfigurator(buildOutputCleanupRegistry, objectFactory).configureKotlinPlatform(project, extension)
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            KotlinCommonSourceSetProcessor(project, sourceSet, tasksProvider, sourceSetContainer)
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
        project: Project,
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