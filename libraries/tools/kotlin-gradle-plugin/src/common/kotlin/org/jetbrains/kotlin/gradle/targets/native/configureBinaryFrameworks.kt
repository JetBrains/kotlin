/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.io.File

/**
 * Contains common data between frameworks that can be bundled to a fat framework.
 */
private data class FrameworkGroupDescription(
    val frameworkName: String,
    val targetFamilyName: String,
    val baseName: String,
    val buildType: NativeBuildType,
)

private val Framework.frameworkGroupDescription
    get() = FrameworkGroupDescription(
        frameworkName = name,
        targetFamilyName = target.konanTarget.family.name.toLowerCaseAsciiOnly(),
        baseName = baseName,
        buildType = buildType
    )

internal fun Project.createFrameworkArtifact(binaryFramework: Framework, linkTask: TaskProvider<KotlinNativeLink>) {
    val frameworkConfiguration = configurations.findConsumable(binaryFramework.binaryFrameworkConfigurationName)
        ?: configurations.createConsumable(binaryFramework.binaryFrameworkConfigurationName).also {
            it.applyBinaryFrameworkGroupAttributes(project, binaryFramework.frameworkGroupDescription, listOf(binaryFramework.target))
            project.launchInStage(KotlinPluginLifecycle.Stage.FinaliseDsl) {
                binaryFramework.copyAttributesTo(project, dest = it)
            }
        }

    // Can't use flatMap here because of https://github.com/gradle/gradle/issues/25645
    val linkTaskOutputProvider = linkTask.map { it.outputFile.get() }
    addFrameworkArtifact(frameworkConfiguration, linkTaskOutputProvider)
}

internal val CreateFatFrameworksSetupAction = KotlinProjectSetupCoroutine {
    KotlinPluginLifecycle.Stage.FinaliseDsl.await()
    val frameworkGroups = multiplatformExtension.targets
        .filterIsInstance<KotlinNativeTarget>()
        .filter { FatFrameworkTask.isSupportedTarget(it) }
        .flatMap { it.binaries }
        .filterIsInstance<Framework>()
        .groupBy { it.frameworkGroupDescription }
        .filter { (_, frameworks) -> frameworks.size > 1 }

    for ((groupDescription, frameworkGroup) in frameworkGroups) {
        project.createFatFramework(groupDescription, frameworkGroup)
    }
}

private val Framework.binaryFrameworkConfigurationName get() = lowerCamelCaseName(name, target.name)
private val FrameworkGroupDescription.fatFrameworkConfigurationName get() = lowerCamelCaseName(frameworkName, targetFamilyName, "fat")

private fun Configuration.applyBinaryFrameworkGroupAttributes(
    project: Project,
    frameworkDescription: FrameworkGroupDescription,
    targets: List<KotlinNativeTarget>,
) {
    with(attributes) {
        setAttribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
        setAttribute(project.artifactTypeAttribute, KotlinNativeTargetConfigurator.NativeArtifactFormat.FRAMEWORK)
        setAttribute(KotlinNativeTarget.kotlinNativeBuildTypeAttribute, frameworkDescription.buildType.name)
        setAttribute(KotlinNativeTarget.kotlinNativeFrameworkNameAttribute, frameworkDescription.frameworkName)
        setAttributeProvider(project, Framework.frameworkTargets) {
            targets.map { it.konanTarget.name }.toSet()
        }
    }
}

private fun Project.addFrameworkArtifact(configuration: Configuration, artifactFile: Provider<File>) {
    val frameworkArtifact = artifacts.add(configuration.name, artifactFile) { artifact ->
        artifact.name = name
        artifact.extension = "framework"
        artifact.type = "binary"
        artifact.classifier = "framework"
    }
    project.extensions.getByType(org.gradle.api.internal.plugins.DefaultArtifactPublicationSet::class.java)
        .addCandidate(frameworkArtifact)
}

private fun Project.createFatFramework(groupDescription: FrameworkGroupDescription, frameworks: List<Framework>) {
    require(frameworks.size > 1) { "Can't create binary fat framework from a single framework" }
    val fatFrameworkConfigurationName = groupDescription.fatFrameworkConfigurationName
    val fatFrameworkTaskName = "link${fatFrameworkConfigurationName.capitalizeAsciiOnly()}"

    val fatFrameworkTask = if (fatFrameworkTaskName in tasks.names) {
        tasks.named(fatFrameworkTaskName, FatFrameworkTask::class.java)
    } else {
        tasks.register(fatFrameworkTaskName, FatFrameworkTask::class.java) {
            it.baseName = groupDescription.baseName
            it.destinationDirProperty.set(it.defaultDestinationDir.dir(groupDescription.buildType.name.toLowerCaseAsciiOnly()))
        }
    }

    fatFrameworkTask.configure {
        try {
            it.from(frameworks)
        } catch (e: Exception) {
            logger.warn("Cannot make fat framework from frameworks: ${frameworks.map { it.name }}", e)
        }
    }

    val fatFrameworkConfiguration = project.configurations.maybeCreateConsumable(fatFrameworkConfigurationName).also {
        it.applyBinaryFrameworkGroupAttributes(project, groupDescription, targets = frameworks.map(Framework::target))
    }

    addFrameworkArtifact(fatFrameworkConfiguration, fatFrameworkTask.map { it.fatFramework })
}