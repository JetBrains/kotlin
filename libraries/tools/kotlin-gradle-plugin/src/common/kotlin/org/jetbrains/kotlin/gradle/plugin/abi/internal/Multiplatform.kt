/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 * Finalizes the configuration of the report variant for the Kotlin Multiplatform Gradle Plugin.
 */
internal fun AbiValidationExtension.finalizeMultiplatformVariant(
    project: Project,
    targets: NamedDomainObjectCollection<KotlinTarget>,
    abiConfiguration: AbiValidationConfiguration,
) {
    project.processJvmKindTargets(binariesSource.get(), targets, abiConfiguration)
    project.processNonJvmTargets(binariesSource.get(), targets, abiConfiguration)
}


private fun Project.processJvmKindTargets(
    binariesSource: BinariesSource,
    targets: Iterable<KotlinTarget>,
    abiConfiguration: AbiValidationConfiguration
) {
    // if there is only one JVM target then we will follow the shortcut
    val singleJvmTarget = targets.singleOrNull { target -> target.platformType == KotlinPlatformType.jvm && target is KotlinJvmTarget }
    val hasAnyAndroidTarget =
        // Target from AGP or Android multiplatform library (JVM platform type + instance of KotlinMultiplatformAndroidLibraryTarget)
        targets.any { target -> target.platformType == KotlinPlatformType.androidJvm || (target.platformType == KotlinPlatformType.jvm && target !is KotlinJvmTarget) }

    if (singleJvmTarget != null && !hasAnyAndroidTarget) {
        addJvmInputs(
            binariesSource,
            singleJvmTarget.targetName,
            true,
            singleJvmTarget.compilations,
            KotlinCompilation.MAIN_COMPILATION_NAME,
            abiConfiguration
        )
        return
    }

    targets
        .asSequence()
        .filter { target -> target.platformType == KotlinPlatformType.jvm }
        .forEach { target ->
            addJvmInputs(
                binariesSource,
                target.targetName,
                false,
                target.compilations,
                KotlinCompilation.MAIN_COMPILATION_NAME,
                abiConfiguration
            )
        }

    // old android application or android target in old KMP
    targets
        .asSequence()
        .filterIsInstance<KotlinAndroidTarget>()
        .forEach { target ->
            if (binariesSource == BinariesSource.MAVEN_PUBLICATIONS) {
                reportDiagnostic(KotlinToolingDiagnostics.AbiValidationAndroidPublicationNotSupported())
            }

            addJvmInputs(
                binariesSource,
                target.targetName,
                false,
                target.compilations,
                ANDROID_RELEASE_BUILD_TYPE,
                abiConfiguration
            )
        }

    // android multiplatform library
    targets
        .asSequence()
        .filter {
            // old android target type in multiplatform library
            (it.platformType == KotlinPlatformType.jvm && it !is KotlinJvmTarget)
                    // new android target type in multiplatform library
                    || (it.platformType == KotlinPlatformType.androidJvm && it !is KotlinAndroidTarget)
        }
        .forEach { target ->
            if (binariesSource == BinariesSource.MAVEN_PUBLICATIONS) {
                reportDiagnostic(KotlinToolingDiagnostics.AbiValidationAndroidPublicationNotSupported())
            }

            addJvmInputs(
                binariesSource,
                target.targetName,
                false,
                target.compilations,
                KotlinCompilation.MAIN_COMPILATION_NAME,
                abiConfiguration
            )
        }
}


private fun Project.processNonJvmTargets(
    binariesSource: BinariesSource,
    targets: Iterable<KotlinTarget>,
    abiConfiguration: AbiValidationConfiguration
) {
    val bannedInTests = bannedCanonicalTargetsInTest()
    targets
        .asSequence()
        .filter { target -> target.emitsKlib }
        .forEach { target ->
            val klibTarget = target.toKlibTarget()
            launch {
                if (target.targetIsSupported() && klibTarget.customizedName !in bannedInTests) {
                    addKlibInputs(
                        binariesSource,
                        klibTarget,
                        target.compilations,
                        abiConfiguration
                    )
                } else {
                    abiConfiguration.unsupportedTargets.add(klibTarget)
                }
            }
        }
}


private fun Project.addKlibInputs(
    binariesSource: BinariesSource,
    klibTarget: KlibTargetId,
    compilations: NamedDomainObjectContainer<out KotlinCompilation<out Any>>,
    abiConfiguration: AbiValidationConfiguration,
) {
    when (binariesSource) {
        BinariesSource.MAVEN_PUBLICATIONS -> {
            analyzeMavenPublicationForKlib(klibTarget, abiConfiguration)
        }
        BinariesSource.MAIN_COMPILATION -> {
            compilations.withCompilationIfExists(KotlinCompilation.MAIN_COMPILATION_NAME) {
                abiConfiguration.addKlibTarget(klibTarget, output.classesDirs)
            }
        }
        BinariesSource.NON_TEST_COMPILATIONS -> {
            compilations.configureEach { compilation ->
                if (!compilation.compilationName.contains("test", ignoreCase = true)) {
                    abiConfiguration.addKlibTarget(klibTarget, compilation.output.classesDirs)
                }
            }
        }
    }
}

private fun Project.addJvmInputs(
    binariesSource: BinariesSource,
    targetName: String,
    singleTarget: Boolean,
    compilations: NamedDomainObjectContainer<out KotlinCompilation<out Any>>,
    mainCompilationName: String,
    abiConfiguration: AbiValidationConfiguration
) {
    val classfiles = files()
    if (singleTarget) {
        abiConfiguration.addJvmDump(classfiles)
    } else {
        abiConfiguration.addJvmDump(targetName, classfiles)
    }
    when (binariesSource) {
        BinariesSource.MAVEN_PUBLICATIONS -> {
            analyzeMavenPublicationForJvm(classfiles)
        }
        BinariesSource.MAIN_COMPILATION -> {
            compilations.withCompilationIfExists(mainCompilationName) {
                classfiles.from(output.classesDirs)
            }
        }
        BinariesSource.NON_TEST_COMPILATIONS -> {
            compilations.configureEach { compilation ->
                if (!compilation.compilationName.contains("test", ignoreCase = true)) {
                    classfiles.from(compilation.output.classesDirs)
                }
            }
        }
    }
}

private suspend fun KotlinTarget.targetIsSupported(): Boolean = when (this) {
    is KotlinNativeTarget -> crossCompilationOnCurrentHostSupported.await()
    else -> true
}

private fun Project.bannedCanonicalTargetsInTest(): Set<String> {
    val prop = kotlinPropertiesProvider.abiValidationBannedTargets
    prop ?: return emptySet()

    return prop.split(",").map { it.trim() }.toSet()
}

internal fun Project.analyzeMavenPublicationForKlib(
    target: KlibTargetId,
    abiConfiguration: AbiValidationConfiguration,
) {
    val publishingExtension = extensions.findByType(PublishingExtension::class.java)
    if (publishingExtension == null) {
        reportDiagnostic(KotlinToolingDiagnostics.AbiValidationNoPublishPlugin())
    }

    publishingExtension.publications.configureEach { publication ->
        if (publication is MavenPublication && publication.name == target.customizedName) {
            publication.artifacts.configureEach { artifact ->
                if (artifact.classifier == null) {
                    abiConfiguration.addKlibTarget(
                        target,
                        files(artifact.file).builtBy(artifact.buildDependencies)
                    )
                }
            }
        }
    }
}
