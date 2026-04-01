/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.runtime.utils

import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget

internal fun lowerCamelCaseName(vararg nameParts: String?): String {
    val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
    return nonEmptyParts.drop(1).joinToString(
        separator = "",
        prefix = nonEmptyParts.firstOrNull().orEmpty(),
        transform = String::capitalizeDefaultLocale
    )
}

internal fun KotlinJsIrSubTarget.disambiguateCamelCased(vararg names: String?): String =
    lowerCamelCaseName(target.disambiguationClassifier, disambiguationClassifier, *names)