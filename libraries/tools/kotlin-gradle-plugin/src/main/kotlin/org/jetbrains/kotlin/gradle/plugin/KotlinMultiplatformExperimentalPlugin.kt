/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskResolver
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.base.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.*
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCommonTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import java.io.Serializable

internal val Project.multiplatformExtension get(): KotlinMultiplatformExtension? =
    project.extensions.findByName("kotlin") as KotlinMultiplatformExtension

internal class KotlinMultiplatformProjectConfigurator(
    private val project: Project,
    private val objectFactory: ObjectFactory,
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator,
    private val kotlinGradleBuildServices: KotlinGradleBuildServices,
    private val kotlinPluginVersion: String,
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) {
    val kotlinOnlyPlatformConfigurator = KotlinOnlyTargetConfigurator(buildOutputCleanupRegistry, objectFactory)

    private inline fun <reified T : KotlinOnlyTarget> createAndSetupExtension(
        name: String,
        kotlinPlatformType: KotlinPlatformType,
        platformClassifier: String,
        sourceSetContainer: KotlinSourceSetContainer<*>,
        userDefinedId: String? = null
    ): T {
        val extension = objectFactory.newInstance(T::class.java).apply {
            this.targetName = name
            platformType = kotlinPlatformType
            disambiguationClassifier = platformClassifier
            userDefinedPlatformId = userDefinedId
        }
        val multiplatformExtension = project.multiplatformExtension
        (multiplatformExtension as ExtensionAware).extensions.add(platformClassifier, extension)
        kotlinOnlyPlatformConfigurator.configureTarget(project, extension)
        return extension
    }

    fun createCommonExtension(): KotlinOnlyTarget {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val extension = createAndSetupExtension<KotlinOnlyTarget>(
            "kotlinCommon", KotlinPlatformType.common, "common", sourceSets
        )

        val tasksProvider = KotlinCommonTasksProvider()
        configureCompilationDefaults(extension) { compilation: KotlinCompilation ->
            KotlinCommonSourceSetProcessor(project, compilation, tasksProvider, sourceSets, kotlinPluginVersion)
        }

        return extension
    }

    fun createJvmExtension(disambiguationSuffix: String? = null): KotlinMppTarget {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val platformSuffix = disambiguationSuffix?.capitalize().orEmpty()
        val platformName = "kotlinJvm$platformSuffix"
        val platformClassifier = "jvm$platformSuffix"

        val extension = createAndSetupExtension<KotlinMppTarget>(
            platformName, KotlinPlatformType.jvm, platformClassifier, sourceSets
        ).apply {
            projectConfigurator = this@KotlinMultiplatformProjectConfigurator
        }

        val tasksProvider = KotlinTasksProvider()
        configureCompilationDefaults(extension) { compilation: KotlinCompilation ->
            Kotlin2JvmSourceSetProcessor(
                project, tasksProvider, sourceSets, compilation as KotlinJvmCompilation, kotlinPluginVersion
            )
        }

//        linkCommonAndPlatformExtensions(project.multiplatformExtension.targets.common, extension)

        return extension
    }

    fun createJvmWithJavaExtension(): KotlinWithJavaTarget {
        project.plugins.apply(JavaPlugin::class.java)

        val sourceSets = KotlinJavaSourceSetContainer(instantiator, project, fileResolver)
        val platformClassifier = "jvmWithJava"

        val extension = objectFactory.newInstance(KotlinWithJavaTarget::class.java).apply {
            val multiplatformExtension = project.multiplatformExtension
            (multiplatformExtension as ExtensionAware).extensions.add(platformClassifier, this@apply)
             registerKotlinSourceSetsIfAbsent(sourceSets, project.kotlinExtension)
            targetName = "kotlin" + platformClassifier.capitalize()
        }

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { javaSourceSet ->
            project.kotlinExtension.sourceSets.maybeCreate(javaSourceSet.name)
            // fixme setup compilation for this target?
        }

        val tasksProvider = KotlinTasksProvider()

        configureCompilationDefaults(extension) { compilation: KotlinJvmWithJavaCompilation ->
            Kotlin2JvmSourceSetProcessor(
                project, tasksProvider, sourceSets, compilation, kotlinPluginVersion
            )
        }

//        linkCommonAndPlatformExtensions(project.multiplatformExtension.common, extension)

        setupConfigurationsInProjectWithJava(project, extension)

        return extension
    }

    fun createAndroidPlatformExtension(): KotlinAndroidTarget {
        val platformClassifier = "android"

        val sourceSets = KotlinAndroidSourceSetContainer(instantiator, project, fileResolver)

        val extension = objectFactory.newInstance(KotlinAndroidTarget::class.java).apply {
            val multiplatformExtension = project.multiplatformExtension
            (multiplatformExtension as ExtensionAware).extensions.add(platformClassifier, this@apply)
            targetName = "kotlin" + platformClassifier.capitalize()
        }

        val tasksProvider = AndroidTasksProvider()

        KotlinAndroidPlugin.applyToCompilation(project, extension, sourceSets, tasksProvider, kotlinPluginVersion)

//        linkCommonAndPlatformExtensions(project.multiplatformExtension.common, extension)

        return extension
    }

    fun setupConfigurationsInProjectWithJava(project: Project, extension: KotlinWithJavaTarget) {
        project.afterEvaluate {
            listOf(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME).forEach {
                project.configurations.getByName(it).usesPlatformOf(extension)
            }
            extension.compilations.all { kotlinCompilationWithJava ->
                val javaSourceSet = kotlinCompilationWithJava.javaSourceSet
                listOf(javaSourceSet.compileClasspathConfigurationName, javaSourceSet.runtimeClasspathConfigurationName).forEach {
                    project.configurations.getByName(it).usesPlatformOf(extension)
                }
            }
        }
    }

    fun createJsPlatformExtension(disambiguationSuffix: String? = null): KotlinMppTarget {
        val sourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)
        val platformSuffix = disambiguationSuffix?.capitalize().orEmpty()
        val platformName = "kotlinJs$platformSuffix"
        val platformClassifier = "js$platformSuffix"

        val extension = createAndSetupExtension<KotlinMppTarget>(
            platformName, KotlinPlatformType.js, platformClassifier, sourceSets
        ).apply {
            projectConfigurator = this@KotlinMultiplatformProjectConfigurator
        }

        val tasksProvider = Kotlin2JsTasksProvider()
        configureCompilationDefaults(extension) { compilation: KotlinJsCompilation ->
            Kotlin2JsSourceSetProcessor(project, tasksProvider, sourceSets, compilation, kotlinPluginVersion)
        }

//        linkCommonAndPlatformExtensions(project.multiplatformExtension.common , extension)

        return extension
    }

    private inline fun <reified T : KotlinCompilation> configureCompilationDefaults(
        target: KotlinTarget,
        crossinline buildSourceSetProcessor: (T) -> KotlinSourceSetProcessor<*>
    ) {
        target.compilations.all { compilation ->
            compilation as T
            buildSourceSetProcessor(compilation).run()
        }
    }

    protected fun linkCommonAndPlatformExtensions(
        commonExtension: KotlinTarget,
        target: KotlinTarget,
        setupConfigurationRelations: Boolean = true
    ) {
        // TODO this code may not be needed after all, since the source sets are going to be matched with the compatibility rules
//        matchSymmetricallyByNames(, target.sourceSets) {
//                commonSourceSet: KotlinSourceSet, platformSourceSet: KotlinSourceSet ->
//            //todo add the sources to the task as it's done in old MPP?
//            platformSourceSet.kotlin.source(commonSourceSet.kotlin)
//        }
//
//        if (setupConfigurationRelations) {
//            listOf(
//                commonExtension.apiElementsConfigurationName to target.compileConfigurationName,
//                commonExtension.runtimeElementsConfigurationName to target.runtimeOnlyConfigurationName
//            ).forEach { (commonConfigurationName, platformConfigurationName) ->
//                val commonConfiguration = project.configurations.getByName(commonConfigurationName)
//                val platformConfiguration = project.configurations.getByName(platformConfigurationName)
//                platformConfiguration.extendsFrom(commonConfiguration)
//            }
//        }
    }

    internal fun addExternalExpectedByModule(extension: KotlinMppTarget, modulePath: String) {
//        val otherModule = project.project(modulePath)
//
//        //FIXME assumption that the configuration names are the same, and the dependency project is built with experimental plugin
//        lateinit var commonConfigurationName: String
//        project.multiplatformExtension.common { commonConfigurationName = this.apiElementsConfigurationName }
//
//        val otherModuleUsageAttribute = PlatformConfigurationUsage.attributeForModule(otherModule).also {
//            PlatformConfigurationUsage.configureMatchingStrategy(project, it)
//        }
//
//        val otherConfigurationName = "platformDependencies${extension.disambiguationClassifier!!.capitalize()}"
//
//        otherModule.whenEvaluated {
//            otherModule.configurations.create(otherConfigurationName).apply {
//                extendsFrom(otherModule.configurations.getByName(commonConfigurationName))
//                usesPlatformOf(extension)
//                attributes.attribute(otherModuleUsageAttribute, PlatformConfigurationUsage.PLATFORM_DEPENDENCIES)
//            }
//        }
//
//        extension.sourceSets.all { sourceSet ->
//            val configurationsToAffect = listOf(
//                sourceSet.compileClasspathConfigurationName,
//                sourceSet.runtimeClasspathConfigurationName
//            ).map { project.configurations.getByName(it) }
//
//            configurationsToAffect.forEach {
//                it.attributes.attribute(otherModuleUsageAttribute, PlatformConfigurationUsage.PLATFORM_DEPENDENCIES)
//            }
//        }
//
//        otherModule.whenEvaluated {
//            otherModule.multiplatformExtension.common {
//                linkCommonAndPlatformExtensions(this@common, extension)
//            }
//        }
        // fixme unsupported for now
        throw UnsupportedOperationException()
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

        val kotlinSourceSets = KotlinOnlySourceSetContainer(project, fileResolver, instantiator, project.tasks as TaskResolver)

        registerKotlinSourceSetsIfAbsent(kotlinSourceSets, kotlinMultiplatformExtension)
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
        kotlinMultiplatformExtension.common { } // make it configure by default

        configurePublishingWithMavenPublish(project)
    }

    private fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") {
        //FIXME find a better way to get all extensions
        val extensions = (project.multiplatformExtension as ExtensionAware).extensions

        val platformExtensions = extensions.schema.mapNotNull { (name, _) ->
            extensions.getByName(name) as? KotlinTarget
        }

        val platformSoftwareComponent = KotlinPlatformSoftwareComponent(project, platformExtensions)

        project.extensions.configure(PublishingExtension::class.java) { publishing ->
            publishing.publications.create("kotlinCompositeLibrary", MavenPublication::class.java) { publication ->
                publication.artifactId = project.name
                publication.groupId = project.group.toString()
                publication.from(platformSoftwareComponent)
                (publication as MavenPublicationInternal).publishWithOriginalFileName()
            }
        }
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