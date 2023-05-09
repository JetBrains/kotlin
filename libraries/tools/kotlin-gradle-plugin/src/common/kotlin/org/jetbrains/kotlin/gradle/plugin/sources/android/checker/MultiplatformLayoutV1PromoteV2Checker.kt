/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout

/**
 * Promotes MultiplatformAndroidSourceSetLayoutV2 when requirements are met
 */
internal object MultiplatformLayoutV1PromoteV2Checker : KotlinAndroidSourceSetLayoutChecker {
    override fun checkBeforeLayoutApplied(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout
    ) {
        if (target.project.kotlinPropertiesProvider.ignoreMppAndroidSourceSetLayoutVersion) return
        runCatching {
            if (MultiplatformLayoutV2AgpRequirementChecker.isAgpRequirementMet()) {
                diagnosticsCollector.reportOncePerGradleBuild(
                    target.project, KotlinToolingDiagnostics.PromoteAndroidSourceSetLayoutV2Warning()
                )
            }
        }
    }
}
