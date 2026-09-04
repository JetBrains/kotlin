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

internal val kotlinArchiveAppleSourceSets = listOf(
    "appleMain", "iosArm64Main", "macosArm64Main"
)
internal val kotlinArchiveAllSourceSets = listOf(
    "commonMain", "nativeMain", "appleMain", "webMain", "jvmMain",
    "jsMain", "wasmJsMain", "linuxMain", "linuxX64Main", "linuxArm64Main",
    "iosArm64Main", "macosArm64Main",
)
internal val kotlinArchiveResourcesSourceSets = listOf(
    "commonMain", "nativeMain", "jsMain", "wasmJsMain",
    "linuxX64Main", "linuxArm64Main", "iosArm64Main",
    "macosArm64Main",
)


internal fun KGPBaseTest.kotlinArchiveProducer(
    gradleVersion: GradleVersion,
    withAppleTargets: Boolean = true,
): TestProject {

    return project("empty", gradleVersion) {
        addKgpToBuildScriptCompilationClasspath()
        settingsBuildScriptInjection {
            settings.rootProject.name = "producer"
        }
        kotlinArchiveAllSourceSets.filter { withAppleTargets || it !in kotlinArchiveAppleSourceSets }.forEach { sourceSetName ->
            addSourceFile(sourceSetName, "fun $sourceSetName() {}")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                kotlinArchiveTargets(withAppleTargets)

                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }
    }
}

internal fun KotlinMultiplatformExtension.kotlinArchiveTargets(withAppleTargets: Boolean = true) {
    jvm()
    js()
    wasmJs()
    linuxX64()
    linuxArm64()
    if (withAppleTargets) {
        iosArm64()
        macosArm64()
    }
}

internal fun TestProject.addSourceFile(sourceSetName: String, content: String, fileName: String = "$sourceSetName.kt") {
    val sourceFile = kotlinSourcesDir(sourceSetName).resolve(fileName)
    sourceFile.parent.createDirectories()
    sourceFile.writeText("$content\n")
}

internal const val PRODUCER_CINTEROP_NAME = "producerInterop"
internal const val PRODUCER_CINTEROP_PACKAGE = "producercinterop"
internal const val PRODUCER_CINTEROP_FUNCTION = "$PRODUCER_CINTEROP_PACKAGE.producerInteropFunction"
internal const val PRODUCER_RESOURCES_PLACEMENT = "embed/producer"

internal fun TestProject.configureResourcesPublication() {
    kotlinArchiveResourcesSourceSets.forEach { sourceSetName ->
        val resourceFile = projectPath.resolve("src/$sourceSetName/multiplatformResources/$sourceSetName.txt")
        resourceFile.parent.createDirectories()
        resourceFile.writeText("$sourceSetName resource\n")
    }
    buildScriptInjection {
        project.applyMultiplatform {
            publishResourcesOfAllSupportedTargets(project)
        }
    }
}

internal fun TestProject.configureCinteropPublication() {
    buildScriptInjection {
        project.enableCinteropCommonization()
        project.applyMultiplatform {
            targets.withType(KotlinNativeTarget::class.java).configureEach { target ->
                target.createCInterop(PRODUCER_CINTEROP_NAME)
            }
        }
    }
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
