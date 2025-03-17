/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics


import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.validateKgpModelIsUklibCompliantAndCreateKgpFragments

internal val UklibPublicationDiagnosticsSetupAction = KotlinProjectSetupAction {
    when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.StandardKMPPublication -> return@KotlinProjectSetupAction
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> Unit
    }

    /**
     * Check that KGP model is compliant with Uklib limitations; this function does validations as a side effect
     */
    project.launch {
        project.multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()
    }


    // FIXME: Diagnostic about all dependencies in the root source set
}