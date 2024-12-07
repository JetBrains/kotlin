/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1
import org.jetbrains.kotlin.gradle.utils.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.relativeTo

/**
 * Detects and warns about usage of source directories from multiplatform layout version 1
 */
internal object MultiplatformLayoutV2MultiplatformLayoutV1StyleSourceDirUsageChecker : KotlinAndroidSourceSetLayoutChecker {

    @OptIn(ExperimentalPathApi::class)
    override fun checkCreatedSourceSet(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") androidSourceSet: DeprecatedAndroidSourceSet
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

        val rootDirPath = target.project.rootDir.toPath()
        val v1KotlinSourceDir = target.project.file("src/$v1kotlinSourceSetName/kotlin")
        if (v1KotlinSourceDir.exists()) {
            val v2SourceDirToUse = target.project.file("src/${kotlinSourceSet.name}/kotlin")
            diagnosticsCollector.report(
                target.project,
                KotlinToolingDiagnostics.SourceSetLayoutV1StyleDirUsageWarning(
                    v1KotlinSourceDir.toPath().relativeTo(rootDirPath).toString(),
                    layout.name,
                    v2SourceDirToUse.toPath().relativeTo(rootDirPath).toString()
                )
            )
        }
    }
}
