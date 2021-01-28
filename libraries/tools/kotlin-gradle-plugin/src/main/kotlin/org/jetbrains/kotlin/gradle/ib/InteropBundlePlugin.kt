/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ib

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.ib.InteropBundlePlugin.Companion.konanTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.KonanTarget

// TODO SELLMAIR TEST

private const val INTEROP_BUNDLE_EXTENSION_NAME = "interopBundle"

private const val INTEROP_BUNDLE_CONFIGURATION_NAME = "interopBundle"
private const val BUILD_INTEROP_BUNDLE_DIRECTORY_TASK_NAME = "buildInteropBundleDirectory"
private const val BUILD_INTEROP_BUNDLE_KLIB_TASK_NAME = "buildInteropBundleKlib"

internal val ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String::class.java)
internal const val KLIB_ARTIFACT_TYPE = "org.jetbrains.kotlin.klib"
internal const val INTEROP_BUNDLE_DIRECTORY_ARTIFACT_TYPE = "org.jetbrains.kotlin.interopBundle.dir"
internal const val INTEROP_BUNDLE_KLIB_ARTIFACT_TYPE = "org.jetbrains.kotlin.interopBundle.cklib"
internal const val COMMONIZED_INTEROP_BUNDLE_DIRECTORY_ARTIFACT_TYPE = "org.jetbrains.kotlin.commonizedInteropBundle"
internal const val INTEROP_BUNDLE_CKLIB_FILE_EXTENSION = "cklib"

open class InteropBundleExtension(internal val project: Project) {
    val version: Property<String> = project.objects.property(String::class.java)
        .convention(project.provider { project.version.toString() })

    val groupId: Property<String> = project.objects.property(String::class.java)
        .convention(project.provider { project.group.toString() })

    val artifactId: Property<String> = project.objects.property(String::class.java)
        .convention(project.provider { project.name.toString() })
}

open class InteropBundlePlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.create(INTEROP_BUNDLE_EXTENSION_NAME, InteropBundleExtension::class.java, target)
        target.registerKonanTargetConfigurations()
        target.registerInteropBundleConfiguration()
        target.registerInteropBundleTasks()
        target.registerArtifacts()
        target.setupPublication()
    }

    internal companion object {
        val konanTargets: Set<KonanTarget> get() = KonanTarget.predefinedTargets.values.toSet()
    }
}

private fun Project.registerKonanTargetConfigurations() {
    konanTargets.forEach { target ->
        configurations.register(target.name) { configuration ->
            configuration.isCanBeConsumed = false
            configuration.isCanBeResolved = true
            configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
            configuration.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            configuration.attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.name)
        }
    }
}

private fun Project.registerInteropBundleConfiguration() {
    configurations.register(INTEROP_BUNDLE_CONFIGURATION_NAME) { configuration ->
        configuration.isCanBeResolved = false
        configuration.isCanBeConsumed = true
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_API))
    }
}

private fun Project.registerInteropBundleTasks() {
    val buildInteropBundleDirectory = tasks.register(
        BUILD_INTEROP_BUNDLE_DIRECTORY_TASK_NAME, CreateInteropBundleTask::class.java
    ) { task -> task.group = "build" }

    tasks.register(BUILD_INTEROP_BUNDLE_KLIB_TASK_NAME, Zip::class.java) { task ->
        task.group = "build"
        task.dependsOn(buildInteropBundleDirectory)
        task.from(buildInteropBundleDirectory.flatMap { it.outputDirectory })
        task.destinationDirectory.set(file("build"))
        task.archiveBaseName.set(project.name)
        task.archiveExtension.set(INTEROP_BUNDLE_CKLIB_FILE_EXTENSION)
    }

    tasks.register("assemble") { task ->
        task.group = "build"
        task.dependsOn(buildInteropBundleKlib)
    }

    tasks.register("build") { task ->
        task.group = "build"
        task.dependsOn("assemble")
    }
}

private fun Project.registerArtifacts() {
    artifacts.add(interopBundleConfiguration.name, buildInteropBundleKlib.map { it.archiveFile.get().asFile }) { artifact ->
        artifact.builtBy(buildInteropBundleKlib)
        artifact.type = INTEROP_BUNDLE_KLIB_ARTIFACT_TYPE
        artifact.extension = INTEROP_BUNDLE_CKLIB_FILE_EXTENSION
    }
}

private fun Project.setupPublication() {
    plugins.withId("maven-publish") {
        val softwareComponentFactoryClass = SoftwareComponentFactory::class.java
        val softwareComponentFactory = (project as ProjectInternal).services.get(softwareComponentFactoryClass)
        val component = softwareComponentFactory.adhoc("interopBundle").apply {
            addVariantsFromConfiguration(interopBundleConfiguration) {}
        }

        project.extensions.configure(PublishingExtension::class.java) { publishing ->
            publishing.publications.register("interopBundle", MavenPublication::class.java) { publication ->
                publication.from(component)
                afterEvaluate {
                    publication.groupId = interopBundleExtension.groupId.orNull
                    publication.version = interopBundleExtension.version.orNull
                    publication.artifactId = interopBundleExtension.artifactId.orNull
                }
            }
        }
    }
}


private val Project.buildInteropBundleKlib: TaskProvider<Zip>
    get() = tasks.withType(Zip::class.java).named(BUILD_INTEROP_BUNDLE_KLIB_TASK_NAME)

private val Project.interopBundleConfiguration get() = configurations.getByName(INTEROP_BUNDLE_CONFIGURATION_NAME)

private val Project.interopBundleExtension get() = extensions.getByName(INTEROP_BUNDLE_EXTENSION_NAME) as InteropBundleExtension
