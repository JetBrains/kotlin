/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.plugins.JavaBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic

internal val ApplyJavaBasePluginSetupAction = KotlinProjectSetupAction {
    project.plugins.apply(JavaBasePlugin::class.java)
}

internal val DeprecateJavaPluginsApplicationSetupAction = KotlinProjectSetupAction {

    project.plugins.withId("java-library") {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.DeprecatedInKMPJavaPluginsDiagnostic("java-library")
        )
    }

    project.plugins.withId("application") {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.DeprecatedInKMPJavaPluginsDiagnostic("application")
        )
    }

    project.plugins.withId("java") {
        project.reportDiagnostic(
            KotlinToolingDiagnostics.DeprecatedInKMPJavaPluginsDiagnostic("java")
        )
    }
}