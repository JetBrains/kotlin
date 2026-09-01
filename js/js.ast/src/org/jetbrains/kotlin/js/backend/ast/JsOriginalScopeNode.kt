/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.backend.ast.metadata

data class JsOriginalScopeNode(
    var name: String?,
    var kind: String?,
    var startLine: Int,
    var startColumn: Int,
    var endLine: Int,
    var endColumn: Int,
    var variables: List<String>,
    var children: List<JsOriginalScopeNode>,
    var isStackFrame: Boolean,
)
