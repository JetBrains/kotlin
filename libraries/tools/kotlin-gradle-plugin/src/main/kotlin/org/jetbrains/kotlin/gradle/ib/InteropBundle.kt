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
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.ib.InteropBundlePlugin.Companion.konanTargets
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.KonanTarget

private const val INTEROP_BUNDLE_EXTENSION_NAME = "interopBundle"

private const val INTEROP_BUNDLE_CONFIGURATION_NAME = "interopBundle"
private const val CREATE_INTEROP_BUNDLE_TASK_NAME = "createInteropBundle"
private const val CREATE_INTEROP_BUNDLE_KIB_TASK_NAME = "createInteropBundleKib"

internal val ARTIFACT_TYPE_ATTRIBUTE = Attribute.of("artifactType", String::class.java)
internal const val KLIB_ARTIFACT_TYPE = "org.jetbrains.kotlin.klib"
internal const val INTEROP_BUNDLE_ARTIFACT_TYPE = "org.jetbrains.kotlin.interopBundle"
internal const val ZIPPED_INTEROP_BUNDLE_ARTIFACT_TYPE = "org.jetbrains.kotlin.zippedInteropBundle"
internal const val COMMONIZED_INTEROP_BUNDLE_ARTIFACT_TYPE = "org.jetbrains.kotlin.commonizedInteropBundle"
internal const val ZIPPED_INTEROP_BUNDLE_FILE_EXTENSION = "kib"

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
    val createInteropBundleTask = tasks.register(CREATE_INTEROP_BUNDLE_TASK_NAME, CreateInteropBundleTask::class.java) { task ->
        task.group = "build"
    }

    tasks.register(CREATE_INTEROP_BUNDLE_KIB_TASK_NAME, Zip::class.java) { task ->
        task.group = "build"
        task.dependsOn(createInteropBundleTask)
        task.from(createInteropBundleTask.flatMap { it.outputDirectory })
        task.destinationDirectory.set(buildDir)
        task.archiveBaseName.set("interopBundle")
        task.archiveExtension.set(ZIPPED_INTEROP_BUNDLE_FILE_EXTENSION)
    }

    tasks.register("clean", Delete::class.java) { task ->
        task.group = "build"
        task.delete(buildDir)
    }

    tasks.register("assemble") { task ->
        task.group = "build"
        task.dependsOn(CREATE_INTEROP_BUNDLE_KIB_TASK_NAME)
    }

    tasks.register("build") { task ->
        task.group = "build"
        task.dependsOn("assemble")
    }
}

private fun Project.registerArtifacts() {
    artifacts.add(INTEROP_BUNDLE_CONFIGURATION_NAME, createInteropBundleKibTask.flatMap { it.archiveFile }) { artifact ->
        artifact.builtBy(createInteropBundleKibTask)
        artifact.type = ZIPPED_INTEROP_BUNDLE_ARTIFACT_TYPE
        artifact.extension = ZIPPED_INTEROP_BUNDLE_FILE_EXTENSION
    }
}

private fun Project.setupPublication() {
    plugins.withId("maven-publish") {
        val softwareComponentFactoryClass = SoftwareComponentFactory::class.java
        val softwareComponentFactory = (project as ProjectInternal).services.get(softwareComponentFactoryClass)
        val component = softwareComponentFactory.adhoc("interopBundle").apply {
            addVariantsFromConfiguration(interopBundleConfiguration) { details ->
                println(details)
            }
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

private val Project.createInteropBundleKibTask: TaskProvider<Zip>
    get() = tasks.withType(Zip::class.java).named(CREATE_INTEROP_BUNDLE_KIB_TASK_NAME)

private val Project.interopBundleConfiguration get() = configurations.getByName(INTEROP_BUNDLE_CONFIGURATION_NAME)

private val Project.interopBundleExtension get() = extensions.getByName(INTEROP_BUNDLE_EXTENSION_NAME) as InteropBundleExtension
