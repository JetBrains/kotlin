/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout
import java.io.File

/**
 * Will detect usage of "Android Style" source directories (like 'src/main/kotlin') and emit a warning
 */
internal object MultiplatformLayoutV2AndroidStyleSourceDirUsageChecker : KotlinAndroidSourceSetLayoutChecker {

    override fun checkCreatedSourceSet(
        diagnosticReporter: KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        val androidStyleSourceDir = target.project.file("src/${androidSourceSet.name}/kotlin")
        if (androidStyleSourceDir in kotlinSourceSet.kotlin.srcDirs && androidStyleSourceDir.exists()) {
            val kotlinStyleSourceDirToUse = target.project.file("src/${kotlinSourceSet.name}/kotlin")
            diagnosticReporter.warning(AndroidStyleSourceDirUsageDiagnostic(androidStyleSourceDir, kotlinStyleSourceDirToUse))
        }
    }

    data class AndroidStyleSourceDirUsageDiagnostic(
        val androidStyleSourceDirInUse: File,
        val kotlinStyleSourceDirToUse: File
    ) : KotlinAndroidSourceSetLayoutChecker.Diagnostic {
        override val message: String
            get() = """
                Usage of 'Android Style' source directory $androidStyleSourceDirInUse is deprecated.
                Use $kotlinStyleSourceDirToUse instead.
            """.trimIndent()
    }
}
