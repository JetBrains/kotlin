/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import com.android.build.gradle.api.AndroidSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout

internal interface KotlinAndroidSourceSetLayoutChecker {
    fun checkBeforeLayoutApplied(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout
    ) = Unit

    fun checkCreatedSourceSet(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) = Unit
}

/* Composite Implementation */

internal fun KotlinAndroidSourceSetLayoutChecker(
    vararg checkers: KotlinAndroidSourceSetLayoutChecker?
): KotlinAndroidSourceSetLayoutChecker {
    return CompositeKotlinAndroidSourceSetLayoutChecker(checkers.filterNotNull())
}

private class CompositeKotlinAndroidSourceSetLayoutChecker(
    private val checkers: List<KotlinAndroidSourceSetLayoutChecker>
) : KotlinAndroidSourceSetLayoutChecker {

    override fun checkBeforeLayoutApplied(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout
    ) {
        checkers.forEach { checker -> checker.checkBeforeLayoutApplied(diagnosticsCollector, target, layout) }
    }

    override fun checkCreatedSourceSet(
        diagnosticsCollector: KotlinToolingDiagnosticsCollector,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        checkers.forEach { checker ->
            checker.checkCreatedSourceSet(diagnosticsCollector, target, layout, kotlinSourceSet, androidSourceSet)
        }
    }
}
