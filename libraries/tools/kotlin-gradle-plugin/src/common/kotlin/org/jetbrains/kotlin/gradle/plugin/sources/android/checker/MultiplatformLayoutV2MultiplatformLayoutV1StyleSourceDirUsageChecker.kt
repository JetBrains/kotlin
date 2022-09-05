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
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1
import org.jetbrains.kotlin.gradle.utils.androidExtension
import java.io.File

/**
 * Detects and warns about usage of source directories from multiplatform layout version 1
 */
internal object MultiplatformLayoutV2MultiplatformLayoutV1StyleSourceDirUsageChecker : KotlinAndroidSourceSetLayoutChecker {

    override fun checkCreatedSourceSet(
        diagnosticReporter: KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        val v1kotlinSourceSetName = multiplatformAndroidSourceSetLayoutV1.naming.kotlinSourceSetName(
            target.disambiguationClassifier, androidSourceSet.name, kotlinSourceSet.androidSourceSetInfo.androidVariantType
        ) ?: return

        /* Layouts did agree on the name of this KotlinSourceSet -> LGTM */
        if (v1kotlinSourceSetName == kotlinSourceSet.name) return

        /*
        Detect naming ambiguity of old 'androidTest' like source sets:
        If also an android source set with the name of the v1 KotlinSourceSet exists,
        then we expect the 'android style source dir usage checker' to warn about this situation.
         */
        if (target.project.androidExtension.sourceSets.findByName(v1kotlinSourceSetName) != null) return

        val v1KotlinSourceDir = target.project.file("src/$v1kotlinSourceSetName/kotlin")
        if (v1KotlinSourceDir.exists()) {
            diagnosticReporter.warning(
                V1StyleSourceDirUsageDiagnostic(
                    layout = layout,
                    v1StyleSourceDirInUse = v1KotlinSourceDir,
                    v2StyleSourceDirToUse = target.project.file("src/${kotlinSourceSet.name}/kotlin")
                )
            )
        }
    }

    data class V1StyleSourceDirUsageDiagnostic(
        private val layout: KotlinAndroidSourceSetLayout,
        val v1StyleSourceDirInUse: File,
        val v2StyleSourceDirToUse: File
    ) : KotlinAndroidSourceSetLayoutChecker.Diagnostic {
        override val message: String
            get() = """
                Found used source directory $v1StyleSourceDirInUse
                This source directory was supported by: ${multiplatformAndroidSourceSetLayoutV1.name}
                Current KotlinAndroidSourceSetLayout: ${layout.name}
                New source directory is: $v2StyleSourceDirToUse
            """.trimIndent()
    }
}
