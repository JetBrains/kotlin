/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.gradle.api.tasks.TaskDependency
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiDumpTaskImpl
import org.jetbrains.kotlin.gradle.utils.named

/**
 * A class for combining and conveniently configuring a group of tasks created for Application Binary Interface (ABI) validation.
 *
 * All these tasks belong to the same report variant.
 */
internal class AbiValidationTaskSet(project: Project) {
    private val legacyDumpTaskProvider =
        project.tasks.named<KotlinAbiDumpTaskImpl>(KotlinAbiDumpTaskImpl.NAME)
    private val legacyCheckDumpTaskProvider =
        project.tasks.named<KotlinAbiCheckTaskImpl>(KotlinAbiCheckTaskImpl.NAME)

    /**
     * Add declarations for the JVM target when no other JVM targets are present.
     *
     * @param [classfiles] The result of compiling the given target, represented as a collection of class files
     */
    fun addSingleJvmTarget(classfiles: FileCollection) {
        legacyDumpTaskProvider.configure {
            it.jvm.add(KotlinAbiDumpTaskImpl.JvmTargetInfo("", classfiles))
        }
    }

    /**
     * Adds declarations for one of several JVM targets with the name [targetName].
     *
     * @param [classfiles] The result of compiling the given target, represented as a collection of class files
     */
    fun addJvmTarget(targetName: String, classfiles: FileCollection) {
        legacyDumpTaskProvider.configure {
            it.jvm.add(KotlinAbiDumpTaskImpl.JvmTargetInfo(targetName, classfiles))
        }
    }

    /**
     * Adds declarations for a non-JVM target.
     *
     * @param [klibTarget] The target to add
     * @param [klibFiles] files of the unpacked klib containing Kotlin compiled code
     */
    fun addKlibTarget(klibTarget: KlibTargetId, klibFiles: FileCollection) {
        legacyDumpTaskProvider.configure {
            it.klib.add(
                KotlinAbiDumpTaskImpl.KlibTargetInfo(
                    klibTarget.customizedName,
                    klibTarget.targetType.canonicalName,
                    klibFiles
                )
            )
        }
    }

    /**
     * Keeps ABI declarations in a dump file for unsupported targets which were added using [unsupportedTarget].
     */
    fun keepLocallyUnsupportedTargets(keep: Provider<Boolean>) {
        legacyDumpTaskProvider.configure {
            it.keepLocallyUnsupportedTargets.set(keep)
        }
    }

    /**
     * Add dependencies of the actual dump generation task.
     */
    fun addDependencies(dependencies: TaskDependency) {
        legacyDumpTaskProvider.configure {
            it.dependsOn(dependencies)
        }
    }

    /**
     * Marks the specified target as unsupported by the Kotlin compiler on the current host.
     */
    fun unsupportedTarget(klibTarget: KlibTargetId) {
        legacyDumpTaskProvider.configure {
            it.unsupportedTargets.add(klibTarget)
        }
    }
}