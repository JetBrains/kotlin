/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

enum class DiagnosticCheckerFilter(val runCommonCheckers: Boolean, val runExtendedCheckers: Boolean) {
    ONLY_COMMON_CHECKERS(runCommonCheckers = true, runExtendedCheckers = false),
    ONLY_EXTENDED_CHECKERS(runCommonCheckers = false, runExtendedCheckers = true),
    EXTENDED_AND_COMMON_CHECKERS(runCommonCheckers = true, runExtendedCheckers = true),
}