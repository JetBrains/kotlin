/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings

enum class ComposeLanguageFeature(
    val since: LanguageVersion
) {
    DefaultParametersInAbstractFunctions(LanguageVersion.KOTLIN_2_1),
    DefaultParametersInOpenFunctions(LanguageVersion.KOTLIN_2_2),
}

fun LanguageVersionSettings.supportsComposeFeature(feature: ComposeLanguageFeature): Boolean {
    return languageVersion >= feature.since
}