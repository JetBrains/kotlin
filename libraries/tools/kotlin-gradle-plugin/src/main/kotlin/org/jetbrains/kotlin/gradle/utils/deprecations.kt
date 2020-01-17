/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.logging.Logger

const val deprecatedBecauseNoConfigAvoidanceUseProvider = "Using this brings performance issues. " +
        "Use the provider instead to benefit from configuration avoidance."

fun Logger.warnAccessToDeprecatedNoConfigAvoidanceSymbol(name: String, newName: String = "${name}Provider") {
    warn("$name has been deprecated for performance reasons. Please, use $newName instead.")
}