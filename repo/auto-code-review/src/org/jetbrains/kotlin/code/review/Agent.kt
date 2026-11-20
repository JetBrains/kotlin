/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

class AgentInfo(val name: String, val model: String, val effort: String)

interface Agent {
    val info: AgentInfo

    suspend fun checkRule(
        rule: CodeRule,
        changedFiles: List<GitDiff.ChangedFile>,
        diffOrigin: GitDiff.Origin,
    ): AgentResult
}
