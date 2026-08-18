/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

interface JsAstDirective {
    fun shouldRunWithBackend(backend: TargetBackend): Boolean
    fun evaluate(ast: JsNode, sourceFile: File)
}
