/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.konan.target.HostManager

internal val Project.multiplatformExtension get(): KotlinMultiplatformExtension? =
    project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

class KotlinMultiplatformPlugin(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator,
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    private class TargetFromPresetExtension(val targetsContainer: NamedDomainObjectCollection<KotlinTarget>) {
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureClosure: Closure<*>): T =
            fromPreset(preset, name, { ConfigureUtil.configure(configureClosure, this) })

        @JvmOverloads
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureAction: T.() -> Unit = { }): T {
            val target = preset.createTarget(name)
            targetsContainer.add(target)
            target.run(configureAction)
            return target
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply(JavaBasePlugin::class.java)

        val targetsContainer = project.container(KotlinTarget::class.java)
        val targetsFromPreset = TargetFromPresetExtension(targetsContainer)

        project.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
            DslObject(targetsContainer).addConvention("fromPreset", targetsFromPreset)

            targets = targetsContainer
            addExtension("targets", targets)

            presets = project.container(KotlinTargetPreset::class.java)
            addExtension("presets", presets)
        }

        setupDefaultPresets(project)
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
        configureSourceSets(project)

        setUpConfigurationAttributes(project)
        configurePublishingWithMavenPublish(project)

        // set up metadata publishing
        targetsFromPreset.fromPreset(
            KotlinMetadataTargetPreset(project, instantiator, fileResolver, buildOutputCleanupRegistry, kotlinPluginVersion),
            METADATA_TARGET_NAME
        )
    }

    fun setupDefaultPresets(project: Project) {
        with((project.kotlinExtension as KotlinMultiplatformExtension).presets) {
            add(KotlinJvmTargetPreset(project, instantiator, fileResolver, buildOutputCleanupRegistry, kotlinPluginVersion))
            add(KotlinJsTargetPreset(project, instantiator, fileResolver, buildOutputCleanupRegistry, kotlinPluginVersion))
            add(KotlinAndroidTargetPreset(project, kotlinPluginVersion))
            add(KotlinJvmWithJavaTargetPreset(project, kotlinPluginVersion))
            HostManager().targets.forEach { _, target ->
                add(KotlinNativeTargetPreset(target.presetName, project, target, buildOutputCleanupRegistry))
            }
        }
    }

    private fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") {
        val targets = project.multiplatformExtension!!.targets
        val kotlinSoftwareComponent = KotlinSoftwareComponent(project, "kotlin", targets)

        project.afterEvaluate { project -> // Use afterEvaluate because publications configuration is no more lazy since Gradle 4.9
            project.extensions.configure(PublishingExtension::class.java) { publishing ->
                publishing.publications.create("kotlin", MavenPublication::class.java) { publication ->
                    publication.from(kotlinSoftwareComponent)
                    (publication as MavenPublicationInternal).publishWithOriginalFileName()
                    publication.artifactId = project.name
                    publication.groupId = project.group.toString()
                }
            }
        }

        project.components.add(kotlinSoftwareComponent)
    }

    private fun configureSourceSets(project: Project) = with (project.kotlinExtension as KotlinMultiplatformExtension) {
        val production = sourceSets.create(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
        val test = sourceSets.create(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)

        targets.all { target ->
            target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                sourceSets.maybeCreate(mainCompilation.defaultSourceSetName).dependsOn(production)
            }

            target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)?.let { testCompilation ->
                sourceSets.maybeCreate(testCompilation.defaultSourceSetName).dependsOn(test)
            }
        }
    }

    private fun setUpConfigurationAttributes(project: Project) {
        val targets = (project.kotlinExtension as KotlinMultiplatformExtension).targets

        project.afterEvaluate {
            targets.all { target ->
                val mainCompilationAttributes = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.attributes
                    ?: return@all

                fun <T> copyAttribute(key: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
                    to.attribute(key, from.getAttribute(key)!!)
                }

                listOf(
                    target.apiElementsConfigurationName,
                    target.runtimeElementsConfigurationName,
                    target.defaultConfigurationName
                )
                    .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                    .forEach { configuration ->
                        mainCompilationAttributes.keySet().forEach { key ->
                            copyAttribute(key, mainCompilationAttributes, configuration.attributes)
                        }
                    }

                target.compilations.all { compilation ->
                    val compilationAttributes = compilation.attributes

                    compilation.relatedConfigurationNames
                        .mapNotNull { configurationName -> target.project.configurations.findByName(configurationName) }
                        .forEach { configuration ->
                            compilationAttributes.keySet().forEach { key ->
                                copyAttribute(key, compilationAttributes, configuration.attributes)
                            }
                        }
                }
            }
        }
    }

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}