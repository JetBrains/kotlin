/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tooling

import com.android.build.gradle.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.GeneratedSubclass
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmJvmVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmNativeVariantInternal
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariant
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetContainerDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.metadata.isCompatibilityMetadataVariantEnabled
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.jetbrains.kotlin.tooling.toJsonString
import java.io.File
import javax.inject.Inject

internal fun Project.registerBuildKotlinToolingMetadataTask() {
    if (!project.kotlinPropertiesProvider.enableKotlinToolingMetadataArtifact) return
    if (project.pm20ExtensionOrNull != null) buildKotlinToolingMetadataForMainKpmModuleTask
    else buildKotlinToolingMetadataTask
}

internal val Project.buildKotlinToolingMetadataForMainKpmModuleTask: TaskProvider<BuildKotlinToolingMetadataTask.FromKpmModule>?
    get() {
        val mainModule = pm20Extension.modules.getByName(GradleKpmModule.MAIN_MODULE_NAME)
        return mainModule.buildKotlinToolingMetadataTask
    }

private val Project.buildKotlinToolingMetadataForAllKpmModulesTask
    get() = locateOrRegisterTask<Task>(BuildKotlinToolingMetadataTask.defaultTaskName) { task ->
        task.group = "build"
        task.description = "Build metadata json file containing information about the used Kotlin tooling"
    }

/**
 * The default task managed by the Kotlin Gradle plugin or `null` if the task is disabled.
 * @see [PropertiesProvider.enableKotlinToolingMetadataArtifact]
 */
internal val Project.buildKotlinToolingMetadataTask: TaskProvider<BuildKotlinToolingMetadataTask.FromKotlinExtension>?
    get() {
        if (!kotlinPropertiesProvider.enableKotlinToolingMetadataArtifact) return null
        val taskName = BuildKotlinToolingMetadataTask.defaultTaskName
        return locateOrRegisterTask(taskName) { task ->
            task.group = "build"
            task.description = "Build metadata json file containing information about the used Kotlin tooling"
        }
    }

internal val GradleKpmModule.buildKotlinToolingMetadataTask: TaskProvider<BuildKotlinToolingMetadataTask.FromKpmModule>?
    get() {
        if (!project.kotlinPropertiesProvider.enableKotlinToolingMetadataArtifact) return null
        val taskName = BuildKotlinToolingMetadataTask.taskNameForKotlinModule(name)

        return project.locateOrRegisterTask(
            name = taskName,
            args = listOf(this),
            invokeWhenRegistered = { project.buildKotlinToolingMetadataForAllKpmModulesTask.dependsOn(this) },
            configureTask = {
                group = "build"
                description = "Build metadata json file containing information about the used Kotlin tooling in module $name"
            }
        )
    }

@DisableCachingByDefault
abstract class BuildKotlinToolingMetadataTask : DefaultTask() {

    @DisableCachingByDefault
    abstract class FromKpmModule
    @Inject constructor(
        @get:Internal
        val module: GradleKpmModule,
        private val projectLayout: ProjectLayout
    ) : BuildKotlinToolingMetadataTask() {

        override val outputDirectory: File
            get() = projectLayout
                .buildDirectory
                .get()
                .asFile
                .resolve("kotlinToolingMetadata")
                .resolve(module.name)

        override fun buildKotlinToolingMetadata() = module.getKotlinToolingMetadata()
    }

    @DisableCachingByDefault
    abstract class FromKotlinExtension
    @Inject constructor(
        private val projectLayout: ProjectLayout
    ) : BuildKotlinToolingMetadataTask() {

        override val outputDirectory: File get() = projectLayout.buildDirectory.get().asFile.resolve("kotlinToolingMetadata")

        override fun buildKotlinToolingMetadata() = project.kotlinExtension.getKotlinToolingMetadata()
    }

    companion object {
        /**
         * The name of the default task managed by the Kotlin Gradle plugin.
         * If enabled, the Kotlin Gradle plugin will automatically register and manage a task with that name.
         * @see PropertiesProvider.enableKotlinToolingMetadataArtifact
         */
        const val defaultTaskName: String = "buildKotlinToolingMetadata"

        /**
         * The name of the default [FromKpmModule] task of the given [GradleKpmModule]'s name
         */
        fun taskNameForKotlinModule(moduleName: String): String = lowerCamelCaseName(defaultTaskName, moduleName)
    }

    @get:OutputDirectory
    abstract val outputDirectory: File

    @get:Internal /* Covered by 'outputDirectory' */
    val outputFile: File get() = outputDirectory.resolve("kotlin-tooling-metadata.json")

    @get:Internal
    internal val kotlinToolingMetadata by lazy { buildKotlinToolingMetadata() }

    @get:Input
    internal val kotlinToolingMetadataJson
        get() = kotlinToolingMetadata.toJsonString()

    protected abstract fun buildKotlinToolingMetadata(): KotlinToolingMetadata

    @TaskAction
    internal fun createToolingMetadataFile() {
        /* Ensure output directory exists and is empty */
        outputDirectory.mkdirs()
        outputDirectory.listFiles().orEmpty().forEach { file -> file.deleteRecursively() }

        outputFile.writeText(kotlinToolingMetadataJson)
    }
}

private fun GradleKpmModule.getKotlinToolingMetadata(): KotlinToolingMetadata {
    return KotlinToolingMetadata(
        schemaVersion = KotlinToolingMetadata.currentSchemaVersion,
        buildSystem = "Gradle",
        buildSystemVersion = project.gradle.gradleVersion,
        buildPlugin = project.plugins.withType(KotlinBasePluginWrapper::class.java).joinToString(";") { it.javaClass.canonicalName },
        buildPluginVersion = project.getKotlinPluginVersion(),
        projectSettings = project.buildProjectSettings(),
        projectTargets = buildProjectTargets()
    )
}

private fun Project.buildProjectSettings(): KotlinToolingMetadata.ProjectSettings {
    return KotlinToolingMetadata.ProjectSettings(
        isHmppEnabled = project.isKotlinGranularMetadataEnabled,
        isCompatibilityMetadataVariantEnabled = project.isCompatibilityMetadataVariantEnabled,
        isKPMEnabled = pm20ExtensionOrNull != null
    )
}

private fun GradleKpmModule.buildProjectTargets(): List<KotlinToolingMetadata.ProjectTargetMetadata> =
    variants.map { variant ->
        KotlinToolingMetadata.ProjectTargetMetadata(
            target = variant.javaClass.canonicalName,
            platformType = variant.platformType.name,
            extras = KotlinToolingMetadata.ProjectTargetMetadata.Extras(
                jvm = variant.jvmExtrasOrNull(),
                android = variant.androidExtrasOrNull(),
                js = variant.jsExtrasOrNull(),
                native = variant.nativeExtrasOrNull()
            )
        )
    }.distinct() // some variants may look identical. e.g. androidRelease and androidDebug, so just keep one of them.

private fun GradleKpmVariant.jvmExtrasOrNull() =
    when (this) {
        is GradleKpmJvmVariant -> KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras(
            jvmTarget = compilationData.kotlinOptions.jvmTarget,
            withJavaEnabled = false
        )

        else -> null
    }

private fun GradleKpmVariant.androidExtrasOrNull() =
    when (this) {
        else -> null
    }

private fun GradleKpmVariant.jsExtrasOrNull() =
    when (this) {
        else -> null
    }

private fun GradleKpmVariant.nativeExtrasOrNull() =
    when (this) {
        is GradleKpmNativeVariantInternal -> KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras(
            konanTarget = konanTarget.name,
            konanVersion = project.konanVersion.toString(),
            konanAbiVersion = KotlinAbiVersion.CURRENT.toString()
        )

        else -> null
    }

private fun KotlinProjectExtension.getKotlinToolingMetadata(): KotlinToolingMetadata {
    return KotlinToolingMetadata(
        schemaVersion = KotlinToolingMetadata.currentSchemaVersion,
        buildSystem = "Gradle",
        buildSystemVersion = project.gradle.gradleVersion,
        buildPlugin = project.plugins.withType(KotlinBasePluginWrapper::class.java).joinToString(";") { it.javaClass.canonicalName },
        buildPluginVersion = project.getKotlinPluginVersion(),
        projectSettings = buildProjectSettings(),
        projectTargets = buildProjectTargets()
    )
}

private fun KotlinProjectExtension.buildProjectSettings(): KotlinToolingMetadata.ProjectSettings {
    return KotlinToolingMetadata.ProjectSettings(
        isHmppEnabled = project.isKotlinGranularMetadataEnabled,
        isCompatibilityMetadataVariantEnabled = project.isCompatibilityMetadataVariantEnabled,
        isKPMEnabled = false
    )
}

private fun KotlinProjectExtension.buildProjectTargets(): List<KotlinToolingMetadata.ProjectTargetMetadata> {
    val targets = when (this) {
        is KotlinMultiplatformExtension -> this.targets.toSet()
        is KotlinSingleTargetExtension<*> -> setOf(this.target)
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

