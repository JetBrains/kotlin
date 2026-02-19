/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

class MainFunctionCandidate(val packageFqn: String, val mainFunctionTag: String?)

inline fun <T> pickMainFunctionFromCandidates(candidates: List<T>, convertToCandidate: (T) -> MainFunctionCandidate): T? {
    return candidates
        .map { it to convertToCandidate(it) }
        .sortedBy { it.second.packageFqn }
        .firstOrNull { it.second.mainFunctionTag != null }
        ?.first
}
