/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiDumpTaskImpl
import org.jetbrains.kotlin.gradle.utils.named

/**
 * A class for combining and conveniently configuring a group of tasks created for Application Binary Interface (ABI) validation.
 *
 * All these tasks belong to the same report variant.
 */
internal class AbiValidationTaskSet(project: Project, variantName: String) {
    private val legacyDumpTaskProvider =
        project.tasks.named<KotlinLegacyAbiDumpTaskImpl>(KotlinLegacyAbiDumpTaskImpl.nameForVariant(variantName))
    private val legacyCheckDumpTaskProvider =
        project.tasks.named<KotlinLegacyAbiCheckTaskImpl>(KotlinLegacyAbiCheckTaskImpl.nameForVariant(variantName))

    /**
     * Add declarations for the JVM target when no other JVM targets are present.
     *
     * @param [classfiles] The result of compiling the given target, represented as a collection of class files
     */
    fun addSingleJvmTarget(classfiles: FileCollection) {
        legacyDumpTaskProvider.configure {
            it.jvm.add(KotlinLegacyAbiDumpTaskImpl.JvmTargetInfo("", classfiles))
        }
    }

    /**
     * Adds declarations for one of several JVM targets with the name [targetName].
     *
     * @param [classfiles] The result of compiling the given target, represented as a collection of class files
     */
    fun addJvmTarget(targetName: String, classfiles: FileCollection) {
        legacyDumpTaskProvider.configure {
            it.jvm.add(KotlinLegacyAbiDumpTaskImpl.JvmTargetInfo(targetName, classfiles))
        }
    }

    /**
     * Adds declarations for a non-JVM target.
     *
     * @param [klibTarget] The target to add
     * @param [klibFiles] files of the unpacked klib containing Kotlin compiled code
     */
    fun addKlibTarget(klibTarget: KlibTarget, klibFiles: FileCollection) {
        legacyDumpTaskProvider.configure {
            it.klibInput.add(KotlinLegacyAbiDumpTaskImpl.KlibTargetInfo(klibTarget.configurableName, klibTarget.targetName, klibFiles))
        }
    }

    /**
     * Keeps ABI declarations in a dump file for unsupported targets which were added using [unsupportedTarget].
     */
    fun keepUnsupportedTargets(keep: Provider<Boolean>) {
        legacyDumpTaskProvider.configure {
            it.keepUnsupportedTargets.set(keep)
        }
    }

    /**
     * Enables generation of ABI dump files for klib targets.
     */
    fun klibEnabled(isEnabled: Provider<Boolean>) {
        legacyDumpTaskProvider.configure {
            it.klibIsEnabled.set(isEnabled)
        }
    }

    /**
     * Marks the specified target as unsupported by the Kotlin compiler on the current host.
     */
    fun unsupportedTarget(klibTarget: KlibTarget) {
        legacyDumpTaskProvider.configure {
            it.unsupportedTargets.add(klibTarget)
        }
    }

    /**
     * Sets the classpath of the ABI tools dependency for all ABI validation tasks.
     */
    fun setClasspath(toolClasspath: Configuration) {
        legacyDumpTaskProvider.configure {
            it.toolsClasspath.from(toolClasspath)
        }
        legacyCheckDumpTaskProvider.configure {
            it.toolsClasspath.from(toolClasspath)
        }
    }
}