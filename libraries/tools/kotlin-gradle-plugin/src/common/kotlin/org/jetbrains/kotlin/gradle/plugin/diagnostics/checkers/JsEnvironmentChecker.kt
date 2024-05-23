/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics

internal object JsEnvironmentChecker : JsLikeEnvironmentChecker(
    KotlinToolingDiagnostics.JsEnvironmentNotChosenExplicitly,
    { it.platformType == KotlinPlatformType.js },
    listOf(
        "browser()",
        "nodejs()"
    ),
    listOf(
        { it.browserNotConfigured() },
        { it.nodejsNotConfigured() },
    )
)