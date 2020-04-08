/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInspection.ex.Tools
import kotlin.test.Ignore

@Ignore(value = "[VD] disabled temporary for further investigation: too much noise, have no clue how to handle it")
class WholeProjectKotlinInspectionTest : WholeProjectInspectionTest(), WholeProjectKotlinFileProvider {
    override fun isEnabledInspection(tools: Tools) = tools.tool.language in setOf(null, "kotlin", "UAST")
}