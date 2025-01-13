/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Finalizes the configuration of the report variant for the JVM or Android version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationVariantSpec.finalizeJvmOrAndroidVariant(
    project: Project,
    abiClasspath: Configuration,
    target: KotlinTarget,
) {
    val taskSet = AbiValidationTaskSet(project, name)
    taskSet.setClasspath(abiClasspath)

    val classfiles = project.files()
    taskSet.addSingleJvmTarget(classfiles)
    target.compilations.withMainCompilationIfExists {
        classfiles.from(output.classesDirs)
    }
}
