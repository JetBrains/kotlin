/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.compiler.metadata

internal class LineBranchBodyIM(val lines: MutableList<LineInfo>) : BodyIM {
    internal class LineInfo(val lineNumber: Int, val columnStart: Int, val pointId: Int, val branches: MutableList<BranchInfo>)

    internal class BranchInfo(val id: Int, val columnStart: Int, val columnEnd: Int, val pointId: Int)
}
