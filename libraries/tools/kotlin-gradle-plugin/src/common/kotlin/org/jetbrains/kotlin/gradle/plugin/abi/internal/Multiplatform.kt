/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Finalizes the configuration of the report variant for the Kotlin Multiplatform Gradle Plugin.
 */
internal fun finalizeMultiplatformVariant(
    project: Project,
    abiClasspath: Configuration,
    targets: NamedDomainObjectCollection<KotlinTarget>,
    keepLocallyUnsupportedTargets: Provider<Boolean>
) {
    val taskSet = AbiValidationTaskSet(project)
    taskSet.setClasspath(abiClasspath)
    taskSet.keepLocallyUnsupportedTargets(keepLocallyUnsupportedTargets)
    taskSet.klibEnabled(project.provider { true })

    project.processJvmKindTargets(targets, taskSet)
    project.processNonJvmTargets(targets, taskSet)
}


private fun Project.processJvmKindTargets(
    targets: Iterable<KotlinTarget>,
    abiValidationTaskSet: AbiValidationTaskSet
) {
    // if there is only one JVM target then we will follow the shortcut
    val singleJvmTarget = targets.singleOrNull { target -> target.platformType == KotlinPlatformType.jvm }
    if (singleJvmTarget != null && targets.none { target -> target.platformType == KotlinPlatformType.androidJvm }) {
        val classfiles = files()
        abiValidationTaskSet.addSingleJvmTarget(classfiles)

        singleJvmTarget.compilations.withCompilationIfExists(KotlinCompilation.MAIN_COMPILATION_NAME) {
            classfiles.from(output.classesDirs)
        }
        return
    }

    targets
        .asSequence()
        .filter { target -> target.platformType == KotlinPlatformType.jvm }
        .forEach { target ->
            val classfiles = files()
            abiValidationTaskSet.addJvmTarget(target.targetName, classfiles)

            target.compilations.withCompilationIfExists(KotlinCompilation.MAIN_COMPILATION_NAME) {
                classfiles.from(output.classesDirs)
            }
        }

    targets
        .asSequence()
        .filterIsInstance<KotlinAndroidTarget>()
        .forEach { target ->
            val classfiles = files()
            abiValidationTaskSet.addJvmTarget(target.targetName, classfiles)

            target.compilations.withCompilationIfExists(ANDROID_RELEASE_BUILD_TYPE) {
                classfiles.from(output.classesDirs)
            }
        }
}


private fun Project.processNonJvmTargets(
    targets: Iterable<KotlinTarget>,
    abiValidationTaskSet: AbiValidationTaskSet
) {
    val bannedInTests = bannedCanonicalTargetsInTest()
    targets
        .asSequence()
        .filter { target -> target.emitsKlib }
        .forEach { target ->
            val klibTarget = target.toKlibTarget()
            launch {
                if (target.targetIsSupported() && klibTarget.configurableName !in bannedInTests) {
                    target.compilations.withCompilationIfExists(KotlinCompilation.MAIN_COMPILATION_NAME) {
                        abiValidationTaskSet.addKlibTarget(klibTarget, output.classesDirs)
                    }
                } else {
                    abiValidationTaskSet.unsupportedTarget(klibTarget)
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
