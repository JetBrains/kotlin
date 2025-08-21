/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo

fun CallInfo.isInsideFirScript(): Boolean {
    return containingFile.isInsideFirScript()
}

@OptIn(DirectDeclarationsAccess::class)
fun FirFile.isInsideFirScript(): Boolean {
    val declarations = declarations
    return declarations.isNotEmpty() && declarations.first() is FirScript
}