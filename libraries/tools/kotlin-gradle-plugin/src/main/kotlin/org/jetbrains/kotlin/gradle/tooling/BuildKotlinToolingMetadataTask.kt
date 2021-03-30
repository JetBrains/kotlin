/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tooling

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.GeneratedSubclass
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.toJsonString
import java.io.File

/**
 * The default task managed by the Kotlin Gradle plugin or `null` if the task is disabled.
 * @see [PropertiesProvider.enableKotlinToolingMetadataArtifact]
 */
internal val Project.buildKotlinToolingMetadataTask: TaskProvider<BuildKotlinToolingMetadataTask>?
    get() {
        if (!PropertiesProvider(this).enableKotlinToolingMetadataArtifact) return null
        locateTask<BuildKotlinToolingMetadataTask>(BuildKotlinToolingMetadataTask.defaultTaskName)?.let { return it }
        return registerTask(BuildKotlinToolingMetadataTask.defaultTaskName) { task ->
            task.group = "build"
            task.description = "Build metadata json file containing information about the used Kotlin tooling"
        }
    }

open class BuildKotlinToolingMetadataTask : DefaultTask() {

    companion object {
        /**
         * The name of the default task managed by the Kotlin Gradle plugin.
         * If enabled, the Kotlin Gradle plugin will automatically register and manage a task with that name.
         * @see PropertiesProvider.enableKotlinToolingMetadataArtifact
         */
        const val defaultTaskName: String = "buildKotlinToolingMetadata"
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
        schemaVersion = KotlinToolingMetadata.currentSchemaVersion,
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
        target = buildTargetString(target),
        platformType = target.platformType.name,
        extras = buildTargetMetadataExtras(target)
    )
}

private fun buildTargetString(target: KotlinTarget): String {
    return if (target is GeneratedSubclass) {
        return target.publicType().canonicalName
    } else target.javaClass.canonicalName
}

private fun buildTargetMetadataExtras(target: KotlinTarget): KotlinToolingMetadata.ProjectTargetMetadata.Extras {
    return KotlinToolingMetadata.ProjectTargetMetadata.Extras(
        jvm = buildJvmExtrasOrNull(target),
        android = buildAndroidExtrasOrNull(target),
        js = buildJsExtrasOrNull(target),
        native = buildNativeExtrasOrNull(target)
    )
}

private fun buildJvmExtrasOrNull(target: KotlinTarget): KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras? {
    if (target !is KotlinJvmTarget) return null
    return KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras(
        withJavaEnabled = target.withJavaEnabled,
        jvmTarget = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)?.kotlinOptions?.jvmTarget
    )
}

private fun buildAndroidExtrasOrNull(target: KotlinTarget): KotlinToolingMetadata.ProjectTargetMetadata.AndroidExtras? {
    if (target !is KotlinAndroidTarget) return null
    val androidExtension = target.project.extensions.findByType(BaseExtension::class.java)
    return KotlinToolingMetadata.ProjectTargetMetadata.AndroidExtras(
        sourceCompatibility = androidExtension?.compileOptions?.sourceCompatibility.toString(),
        targetCompatibility = androidExtension?.compileOptions?.targetCompatibility.toString()
    )
}

private fun buildJsExtrasOrNull(target: KotlinTarget): KotlinToolingMetadata.ProjectTargetMetadata.JsExtras? {
    if (target !is KotlinJsSubTargetContainerDsl) return null
    return KotlinToolingMetadata.ProjectTargetMetadata.JsExtras(
        isBrowserConfigured = target.isBrowserConfigured,
        isNodejsConfigured = target.isNodejsConfigured
    )
}

private fun buildNativeExtrasOrNull(target: KotlinTarget): KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras? {
    if (target !is KotlinNativeTarget) return null
    return KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras(
        konanTarget = target.konanTarget.name,
        konanVersion = target.project.konanVersion.toString(),
        konanAbiVersion = KotlinAbiVersion.CURRENT.toString()
    )
}

