/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tooling

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.toJsonString
import java.io.File

open class BuildKotlinToolingMetadataTask : DefaultTask() {

    companion object {
        const val defaultTaskName: String = "buildKotlinToolingMetadata"
    }

    init {
        group = "build"
        description = "Build metadata json file containing information about the used Kotlin tooling"
    }


    @get:OutputFile
    val outputFile: Property<File> = project.objects.property(File::class.java)
        .convention(project.buildDir.resolve("kotlinToolingMetadata").resolve("kotlin-tooling-metadata.json"))

    @Internal
    fun getKotlinToolingMetadata(): KotlinToolingMetadata {
        return project.kotlinExtension.getKotlinToolingMetadata()
    }

    @Input
    internal fun getKotlinToolingMetadataJson(): String = getKotlinToolingMetadata().toJsonString()

    @TaskAction
    internal fun createToolingMetadataFile() {
        val outputFile = outputFile.orNull ?: return
        outputFile.parentFile.mkdirs()
        outputFile.writeText(getKotlinToolingMetadataJson())
    }
}

private fun KotlinProjectExtension.getKotlinToolingMetadata(): KotlinToolingMetadata {
    return KotlinToolingMetadata(
        buildSystem = "Gradle",
        buildSystemVersion = project.gradle.gradleVersion,
        buildPlugin = project.plugins.withType(KotlinBasePluginWrapper::class.java).joinToString(";") { it.javaClass.canonicalName },
        buildPluginVersion = project.getKotlinPluginVersion() ?: throw IllegalStateException(
            "Failed to infer Kotlin Plugin version"
        ),
        projectSettings = buildProjectSettings(),
        projectTargets = buildProjectTargets()
    )
}

private fun KotlinProjectExtension.buildProjectSettings(): KotlinToolingMetadata.ProjectSettings {
    return KotlinToolingMetadata.ProjectSettings(
        isHmppEnabled = project.isKotlinGranularMetadataEnabled,
        isCompatibilityMetadataVariantEnabled = project.isCompatibilityMetadataVariantEnabled
    )
}

private fun KotlinProjectExtension.buildProjectTargets(): List<KotlinToolingMetadata.ProjectTargetMetadata> {
    val targets = when (this) {
        is KotlinMultiplatformExtension -> this.targets.toSet()
        is KotlinSingleTargetExtension -> setOf(this.target)
        else -> emptySet()
    }

    return targets.map { target -> buildTargetMetadata(target) }
}

private fun buildTargetMetadata(target: KotlinTarget): KotlinToolingMetadata.ProjectTargetMetadata {
    return KotlinToolingMetadata.ProjectTargetMetadata(
        target = target.javaClass.canonicalName,
        platformType = target.platformType.name,
        extras = buildTargetMetadataExtras(target)
    )
}

private fun buildTargetMetadataExtras(target: KotlinTarget): Map<String, String> {
    val extras = mutableMapOf<String, String>()
    when (target) {
        is KotlinJvmTarget -> {
            extras["withJavaEnabled"] = target.withJavaEnabled.toString()
            target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.let { mainCompilation ->
                extras["jvmTarget"] = mainCompilation.kotlinOptions.jvmTarget
            }
        }
        is KotlinAndroidTarget -> {
            val androidExtension = target.project.extensions.findByType(BaseExtension::class.java)
            extras["sourceCompatibility"] = androidExtension?.compileOptions?.sourceCompatibility.toString()
            extras["targetCompatibility"] = androidExtension?.compileOptions?.targetCompatibility.toString()
        }
        is KotlinJsSubTargetContainerDsl -> {
            extras["isBrowserConfigured"] = target.isBrowserConfigured.toString()
            extras["isNodejsConfigured"] = target.isNodejsConfigured.toString()
        }
        is KotlinNativeTarget -> {
            extras["konanTarget"] = target.konanTarget.name
        }
    }
    return extras.toMap()
}
