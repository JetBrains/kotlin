/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ImmediateDiagnosticReporting
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticImmediately

internal object KotlinCompilerEmbeddableCheck {
    fun Project.checkCompilerEmbeddableInClasspath() {
        // check that class from kotlin-compiler-embeddable is not accessible from the plugin classloader
        val present =
            KotlinCompilerEmbeddableCheck::class.java.classLoader.getResource(CLASS_FROM_KCE_FQN) != null
        if (!present) return

        // this diagnostic should be reported ASAP as possible incompatibilities may fail the build very early
        @OptIn(ImmediateDiagnosticReporting::class)
        reportDiagnosticImmediately(
            KotlinToolingDiagnostics.KotlinCompilerEmbeddableIsPresentInClasspath()
        )
    }

    /*
    * There's no proper way to exclude string constant from being relocated by shadow,
    * https://github.com/GradleUp/shadow/issues/232,
    * so the chosen class should not be included into KGP and relocated by shadow plugin.
    */
    private const val CLASS_FROM_KCE_FQN = "org/jetbrains/kotlin/asJava/KotlinAsJavaSupport.class"
}