/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android.checker

import com.android.build.gradle.api.AndroidSourceSet
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.utils.SingleWarningPerBuild

internal interface KotlinAndroidSourceSetLayoutChecker {
    open class ProjectMisconfiguredException(message: String) : Exception(message)

    interface DiagnosticReporter {
        fun error(diagnostic: Diagnostic): Nothing
        fun warning(diagnostic: Diagnostic)

        companion object {
            fun create(project: Project, logger: Logger, layout: KotlinAndroidSourceSetLayout): DiagnosticReporter =
                DiagnosticReporterImpl(project, logger, layout)
        }
    }

    interface Diagnostic {
        val message: String
    }

    fun checkBeforeLayoutApplied(
        diagnosticReporter: DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout
    ) = Unit

    fun checkCreatedSourceSet(
        diagnosticReporter: DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) = Unit
}

private class DiagnosticReporterImpl(
    private val project: Project,
    private val logger: Logger,
    private val layout: KotlinAndroidSourceSetLayout
) : KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter {
    override fun error(diagnostic: KotlinAndroidSourceSetLayoutChecker.Diagnostic): Nothing {
        throw KotlinAndroidSourceSetLayoutChecker.ProjectMisconfiguredException("${layout.name}: ${diagnostic.message}")
    }

    override fun warning(diagnostic: KotlinAndroidSourceSetLayoutChecker.Diagnostic) {
        SingleWarningPerBuild.show(project, logger, "w: ${layout.name}: ${diagnostic.message}\n")
    }
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
        diagnosticReporter: KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout
    ) {
        checkers.forEach { checker -> checker.checkBeforeLayoutApplied(diagnosticReporter, target, layout) }
    }

    override fun checkCreatedSourceSet(
        diagnosticReporter: KotlinAndroidSourceSetLayoutChecker.DiagnosticReporter,
        target: KotlinAndroidTarget,
        layout: KotlinAndroidSourceSetLayout,
        kotlinSourceSet: KotlinSourceSet,
        androidSourceSet: AndroidSourceSet
    ) {
        checkers.forEach { checker ->
            checker.checkCreatedSourceSet(diagnosticReporter, target, layout, kotlinSourceSet, androidSourceSet)
        }
    }
}
