/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.abi.utils

import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.testbase.GradleProject
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import java.io.File

/**
 * Configures ABI validation in specified in [T] extension.
 */
internal fun <T : AbiValidationVariantSpec> GradleProject.abiValidation(configuration: T.() -> Unit) {
    buildScriptInjection {
        @Suppress("UNCHECKED_CAST")
        ((project.extensions.getByName("kotlin") as ExtensionAware).extensions.getByName("abiValidation") as T).configuration()
    }
}

/**
 * Gets the reference dump file for the specified [variant] in a Kotlin JVM project or a Kotlin Multiplatform project without any Android target.
 */
internal fun GradleProject.referenceJvmDumpFile(variant: String = "main"): File {
    val dumpDir = if (variant == "main") "api" else "api-$projectName"
    return projectPath.resolve(dumpDir).resolve("$projectName.api").toFile()
}

/**
 * Gets the reference dump file for the JVM target of the specified [variant] in a Kotlin Multiplatform project with a mix of JVM and Android targets.
 */
internal fun GradleProject.referenceMixedJvmDumpFile(variant: String = "main"): File {
    val dumpDir = if (variant == "main") "api" else "api-$projectName"
    return projectPath.resolve(dumpDir).resolve("jvm").resolve("$projectName.api").toFile()
}

/**
 * Gets the reference dump file for the Android target of the specified [variant] in a Kotlin Multiplatform project with a mix of JVM and Android targets.
 */
internal fun GradleProject.referenceMixedAndroidDumpFile(variant: String = "main"): File {
    val dumpDir = if (variant == "main") "api" else "api-$projectName"
    return projectPath.resolve(dumpDir).resolve("android").resolve("$projectName.api").toFile()
}
