/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.archive

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.uklibs.enableCinteropCommonization
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.addKgpToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import java.io.File
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * All the source sets of the [kotlinArchiveProducer], both platform and shared ones.
 */
internal val kotlinArchiveSourceSets = listOf(
    "commonMain",
    "nativeMain",
    "appleMain",
    "webMain",
    "jvmMain",
    "jsMain",
    "wasmJsMain",
    "linuxX64Main",
    "iosArm64Main",
    "macosArm64Main",
)

/**
 * A producer with a target of every kind supported by the Kotlin Archive and with
 * a single declaration in every source set, so that the published sources can be checked.
 */
internal fun KGPBaseTest.kotlinArchiveProducer(gradleVersion: GradleVersion): TestProject = project("empty", gradleVersion) {
    addKgpToBuildScriptCompilationClasspath()
    settingsBuildScriptInjection {
        settings.rootProject.name = "producer"
    }
    kotlinArchiveSourceSets.forEach { sourceSetName ->
        addSourceFile(sourceSetName, "fun $sourceSetName() {}")
    }
    buildScriptInjection {
        project.applyMultiplatform {
            kotlinArchiveTargets()

            publishing {
                publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
            }
        }
    }
}

/**
 * The targets of the [kotlinArchiveProducer], which have to be repeated by its consumers.
 */
internal fun KotlinMultiplatformExtension.kotlinArchiveTargets() {
    jvm()
    js()
    wasmJs()
    linuxX64()
    iosArm64()
    macosArm64()
}

internal fun TestProject.addSourceFile(sourceSetName: String, content: String) {
    val sourceFile = kotlinSourcesDir(sourceSetName).resolve("$sourceSetName.kt")
    sourceFile.parent.createDirectories()
    sourceFile.writeText("$content\n")
}

/**
 * The name of the cinterop published by the [kotlinArchiveComplexProducer] for all its native targets.
 *
 * As the same cinterop is defined for all of them, it is commonized for the shared native source sets.
 */
internal const val PRODUCER_CINTEROP_NAME = "producerInterop"

/**
 * The package the [PRODUCER_CINTEROP_NAME] declarations are generated into.
 */
internal const val PRODUCER_CINTEROP_PACKAGE = "producercinterop"

/**
 * The function declared by the [PRODUCER_CINTEROP_NAME] cinterop.
 */
internal const val PRODUCER_CINTEROP_FUNCTION = "$PRODUCER_CINTEROP_PACKAGE.producerInteropFunction"

/**
 * The source sets of the [kotlinArchiveComplexProducer] which have multiplatform resources.
 *
 * Resources are not published for the jvm target, so `jvmMain` is not there.
 */
internal val kotlinArchiveResourceSourceSets = listOf(
    "commonMain",
    "nativeMain",
    "jsMain",
    "wasmJsMain",
    "linuxX64Main",
    "iosArm64Main",
    "macosArm64Main",
)

/**
 * The directory the producer resources are placed into by the consumers.
 */
internal const val PRODUCER_RESOURCES_PLACEMENT = "embed/producer"

/**
 * The [kotlinArchiveProducer] extended with everything else the Kotlin Archive stores:
 * multiplatform resources and cinterops, commonized for the shared native source sets.
 */
internal fun KGPBaseTest.kotlinArchiveComplexProducer(gradleVersion: GradleVersion): TestProject = project("empty", gradleVersion) {
    addKgpToBuildScriptCompilationClasspath()
    settingsBuildScriptInjection {
        settings.rootProject.name = "producer"
    }
    kotlinArchiveSourceSets.forEach { sourceSetName ->
        addSourceFile(sourceSetName, "fun $sourceSetName() {}")
    }
    kotlinArchiveResourceSourceSets.forEach { sourceSetName ->
        addResourceFile(sourceSetName)
    }
    buildScriptInjection {
        project.enableCinteropCommonization()
        project.applyMultiplatform {
            kotlinArchiveTargets()

            targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                target.createCInterop(PRODUCER_CINTEROP_NAME)
            }
            publishResourcesOfAllSupportedTargets(project)

            publishing {
                publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
            }
        }
    }
}

/**
 * A resource file named after the source set it belongs to, so that it is visible in which source set
 * every resolved resource is coming from.
 */
internal fun TestProject.addResourceFile(sourceSetName: String) {
    val resourceFile = projectPath.resolve("src/$sourceSetName/multiplatformResources/$sourceSetName.txt")
    resourceFile.parent.createDirectories()
    resourceFile.writeText("$sourceSetName resource\n")
}

private fun KotlinMultiplatformExtension.publishResourcesOfAllSupportedTargets(project: Project) {
    val resourcesPublication = project.extraProperties.get(
        KotlinTargetResourcesPublication.EXTENSION_NAME
    ) as KotlinTargetResourcesPublication

    targets.matching { resourcesPublication.canPublishResources(it) }.configureEach { target ->
        resourcesPublication.publishResourcesAsKotlinComponent(
            target = target,
            resourcePathForSourceSet = { sourceSet ->
                KotlinTargetResourcesPublication.ResourceRoot(
                    resourcesBaseDirectory = project.provider { project.file("src/${sourceSet.name}/multiplatformResources") },
                    includes = emptyList(),
                    excludes = emptyList(),
                )
            },
            relativeResourcePlacement = project.provider { File(PRODUCER_RESOURCES_PLACEMENT) },
        )
    }
}

private fun KotlinNativeTarget.createCInterop(interopName: String) {
    val definitionFile = project.layout.projectDirectory.file("$interopName.def")
    definitionFile.asFile.writeText(
        """
        language = C
        package = $PRODUCER_CINTEROP_PACKAGE
        ---
        void ${interopName}Function(void);
        """.trimIndent()
    )
    compilations.getByName("main").cinterops.create(interopName) {
        it.definitionFile.set(definitionFile)
    }
}
