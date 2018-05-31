/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.*
import org.gradle.api.internal.attributes.CompatibilityRule
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.base.*
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinOnlySourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinOnlySourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCommonTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.matchSymmetricallyByNames
import java.io.Serializable

private val Project.multiplatformExtension get() = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

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

    private inline fun <reified T : KotlinOnlyPlatformExtension> createAndSetupExtension(
        name: String,
        kotlinPlatformType: KotlinPlatformType,
        platformClassifier: String,
        sourceSetContainer: KotlinSourceSetContainer<*>,
        userDefinedId: String? = null
    ): T {
        val extension = objectFactory.newInstance(T::class.java).apply {
            platformName = name
            platformType = kotlinPlatformType
            platformDisambiguationClassifier = platformClassifier
            userDefinedPlatformId = userDefinedId
        }
        val multiplatformExtension = project.multiplatformExtension
        (multiplatformExtension as ExtensionAware).extensions.add(platformClassifier, extension)
        registerKotlinSourceSetsIfAbsent(sourceSetContainer, extension)
        kotlinOnlyPlatformConfigurator.configureKotlinPlatform(project, extension)
        return extension
    }

    fun createCommonExtension(): KotlinOnlyPlatformExtension {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val extension = createAndSetupExtension<KotlinOnlyPlatformExtension>(
            "kotlinCommon", KotlinPlatformType.COMMON, "common", sourceSets
        )

        val tasksProvider = KotlinCommonTasksProvider()
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            KotlinCommonSourceSetProcessor(project, sourceSet, tasksProvider, sourceSets, extension)
        }

        return extension
    }

    fun createJvmExtension(disambiguationSuffix: String? = null): KotlinMppPlatformExtension {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val platformSuffix = disambiguationSuffix?.capitalize().orEmpty()
        val platformName = "kotlinJvm$platformSuffix"
        val platformClassifier = "jvm$platformSuffix"

        val extension = createAndSetupExtension<KotlinMppPlatformExtension>(
            platformName, KotlinPlatformType.JVM, platformClassifier, sourceSets
        ).apply {
            projectConfigurator = this@KotlinMultiplatformProjectConfigurator
        }

        val tasksProvider = KotlinTasksProvider()
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            Kotlin2JvmSourceSetProcessor(
                project, sourceSet, tasksProvider, sourceSets, kotlinPluginVersion, kotlinGradleBuildServices, extension
            )
        }

        project.multiplatformExtension.common {
            linkCommonAndPlatformExtensions(this@common, extension)
        }

        return extension
    }

    fun createJsPlatformExtension(disambiguationSuffix: String? = null): KotlinMppPlatformExtension {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val platformSuffix = disambiguationSuffix?.capitalize().orEmpty()
        val platformName = "kotlinJs$platformSuffix"
        val platformClassifier = "js$platformSuffix"

        val extension = createAndSetupExtension<KotlinMppPlatformExtension>(
            platformName, KotlinPlatformType.JS, platformClassifier, sourceSets
        ).apply {
            projectConfigurator = this@KotlinMultiplatformProjectConfigurator
        }

        val tasksProvider = Kotlin2JsTasksProvider()
        configureSourceSetDefaults(extension) { sourceSet: KotlinOnlySourceSet ->
            Kotlin2JsSourceSetProcessor(project, sourceSet, tasksProvider, sourceSets, extension)
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
        platformExtension: KotlinPlatformExtension,
        setupConfigurationRelations: Boolean = true
    ) {
        matchSymmetricallyByNames(commonExtension.sourceSets, platformExtension.sourceSets) {
                commonSourceSet: KotlinSourceSet, platformSourceSet: KotlinSourceSet ->
            platformSourceSet.kotlin.source(commonSourceSet.kotlin)
        }

        if (setupConfigurationRelations) {
            listOf(
                commonExtension.apiElementsConfigurationName to platformExtension.compileConfigurationName,
                commonExtension.runtimeElementsConfigurationName to platformExtension.runtimeOnlyConfigurationName
            ).forEach { (commonConfigurationName, platformConfigurationName) ->
                val commonConfiguration = project.configurations.getByName(commonConfigurationName)
                val platformConfiguration = project.configurations.getByName(platformConfigurationName)
                platformConfiguration.extendsFrom(commonConfiguration)
            }
        }
    }

    internal fun addExternalExpectedByModule(extension: KotlinMppPlatformExtension, modulePath: String) {
        val otherModule = project.project(modulePath)

        //FIXME assumption that the configuration names are the same, and the dependency project is built with experimental plugin
        lateinit var commonConfigurationName: String
        project.multiplatformExtension.common { commonConfigurationName = this.apiElementsConfigurationName }

        val otherModuleUsageAttribute = PlatformConfigurationUsage.attributeForModule(otherModule).also {
            PlatformConfigurationUsage.configureMatchingStrategy(project, it)
        }

        val otherConfigurationName = "platformDependencies${extension.platformDisambiguationClassifier!!.capitalize()}"

        otherModule.whenEvaluated {
            otherModule.configurations.create(otherConfigurationName).apply {
                extendsFrom(otherModule.configurations.getByName(commonConfigurationName))
                usesPlatformOf(extension)
                attributes.attribute(otherModuleUsageAttribute, PlatformConfigurationUsage.PLATFORM_DEPENDENCIES)
            }
        }

        extension.sourceSets.all { sourceSet ->
            val configurationsToAffect = listOf(
                sourceSet.compileClasspathConfigurationName,
                sourceSet.runtimeClasspathConfigurationName
            ).map { project.configurations.getByName(it) }

            configurationsToAffect.forEach {
                it.attributes.attribute(otherModuleUsageAttribute, PlatformConfigurationUsage.PLATFORM_DEPENDENCIES)
            }
        }

        otherModule.whenEvaluated {
            otherModule.multiplatformExtension.common {
                linkCommonAndPlatformExtensions(this@common, extension)
            }
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

        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
        kotlinMultiplatformExtension.common { } // make it configure by default
    }
}

internal enum class PlatformConfigurationUsage : Named, Serializable {
    PLATFORM_IMPLEMENTATION, PLATFORM_DEPENDENCIES;

    override fun getName(): String = toString()

    companion object {
        fun attributeForModule(module: Project): Attribute<PlatformConfigurationUsage> =
            Attribute.of("org.jetbrains.kotlin.platformUsageOf${module.path}", PlatformConfigurationUsage::class.java)

        fun configureMatchingStrategy(project: Project, attribute: Attribute<PlatformConfigurationUsage>) {
            // FIXME decide whether we need this
        }
    }
}