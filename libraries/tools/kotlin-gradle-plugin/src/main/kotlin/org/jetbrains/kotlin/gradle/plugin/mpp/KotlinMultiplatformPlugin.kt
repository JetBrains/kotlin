/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet

internal val Project.multiplatformExtension get(): KotlinMultiplatformExtension? =
    project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

internal class KotlinMultiplatformPlugin(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val fileResolver: FileResolver,
    private val instantiator: Instantiator,
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    private class TargetFromPresetExtension(val targetsContainer: NamedDomainObjectCollection<KotlinTarget>) {
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureClosure: Closure<*>): T =
            fromPreset(preset, name, { executeClosure(configureClosure) })

        @JvmOverloads
        fun <T : KotlinTarget> fromPreset(preset: KotlinTargetPreset<T>, name: String, configureAction: T.() -> Unit = { }): T {
            val target = preset.createTarget(name)
            targetsContainer.add(target)
            target.run(configureAction)
            return target
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply(BasePlugin::class.java)

        val targetsContainer = project.container(KotlinTarget::class.java)
        val targetsFromPreset = TargetFromPresetExtension(targetsContainer)

        val kotlinMultiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
            DslObject(targetsContainer).addConvention("fromPreset", targetsFromPreset)
            targets = targetsContainer
            addExtension("targets", targets)

            presets = project.container(KotlinTargetPreset::class.java)
            addExtension("presets", presets)
        }

        setupDefaultPresets(project)
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
        configureSourceSets(project)

        targetsFromPreset.fromPreset(
            kotlinMultiplatformExtension.presets.getByName(KotlinUniversalTargetPreset.PRESET_NAME),
            "universal"
        )

        configurePublishingWithMavenPublish(project)
    }

    fun setupDefaultPresets(project: Project) {
        val container = (project.kotlinExtension as KotlinMultiplatformExtension).presets
        container.add(KotlinUniversalTargetPreset(project, instantiator, fileResolver, buildOutputCleanupRegistry, kotlinPluginVersion))
        container.add(KotlinJvmTargetPreset(project, instantiator, fileResolver, buildOutputCleanupRegistry, kotlinPluginVersion))
        container.add(KotlinJsTargetPreset(project, instantiator, fileResolver, buildOutputCleanupRegistry, kotlinPluginVersion))
    }

    private fun configurePublishingWithMavenPublish(project: Project) = project.pluginManager.withPlugin("maven-publish") {
        val targets = project.multiplatformExtension!!.targets

        val platformSoftwareComponent = KotlinPlatformSoftwareComponent(project, targets)

        project.extensions.configure(PublishingExtension::class.java) { publishing ->
            publishing.publications.create("kotlinCompositeLibrary", MavenPublication::class.java) { publication ->
                publication.artifactId = project.name
                publication.groupId = project.group.toString()
                publication.from(platformSoftwareComponent)
                (publication as MavenPublicationInternal).publishWithOriginalFileName()
            }
        }
    }

    private fun configureSourceSets(project: Project) = with (project.kotlinExtension as KotlinMultiplatformExtension) {
        sourceSets.all { defineSourceSetConfigurations(project, it) }
        val main = sourceSets.create("main")
        val test = sourceSets.create("test")

        targets.all {
            it.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).source(main)
            it.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME).source(test)
        }
    }

    private fun defineSourceSetConfigurations(project: Project, sourceSet: KotlinSourceSet) = with (project.configurations) {
        sourceSet.relatedConfigurationNames.forEach { configurationName ->
            maybeCreate(configurationName)
        }
    }
}