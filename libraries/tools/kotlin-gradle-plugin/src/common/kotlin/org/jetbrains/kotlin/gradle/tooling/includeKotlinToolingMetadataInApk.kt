/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tooling

import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.pm20ExtensionOrNull

internal fun Project.includeKotlinToolingMetadataInApk() {
    plugins.withId("com.android.application") {
        val buildKotlinToolingMetadataTask = if (project.pm20ExtensionOrNull != null) {
            buildKotlinToolingMetadataForMainKpmModuleTask
        } else {
            buildKotlinToolingMetadataTask
        } ?: return@withId

        val android = project.extensions.getByName("android") as AppExtension
        android.applicationVariants.configureEach { variant ->
            if (!variant.buildType.isDebuggable) {
                variant.registerPostJavacGeneratedBytecode(
                    files(buildKotlinToolingMetadataTask.map { it.outputDirectory })
                )
            }
        }
    }
}
