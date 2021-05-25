/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import org.jetbrains.uast.UFile
import org.jetbrains.uast.evaluation.UEvaluatorExtension
import org.jetbrains.uast.evaluation.analyzeAll

fun UFile.asLogValues(
    uEvaluatorExtension: UEvaluatorExtension? = null
): String {
    val evaluationContext = analyzeAll(extensions = uEvaluatorExtension?.let { listOf(it) } ?: emptyList())
    return ValueLogger(evaluationContext).apply {
        this@asLogValues.accept(this)
    }.toString()
}
