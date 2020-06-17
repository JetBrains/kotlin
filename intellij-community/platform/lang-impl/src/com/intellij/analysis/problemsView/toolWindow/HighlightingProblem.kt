// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import javax.swing.Icon

internal class HighlightingProblem(private val highlighter: RangeHighlighterEx) : Problem {

  private fun getIcon(level: HighlightDisplayLevel) = if (severity >= level.severity.myVal) level.icon else null

  internal val info: HighlightInfo?
    get() = HighlightInfo.fromRangeHighlighter(highlighter)

  override val icon: Icon
    get() = HighlightDisplayLevel.find(info?.severity)?.icon
            ?: getIcon(HighlightDisplayLevel.ERROR)
            ?: getIcon(HighlightDisplayLevel.WARNING)
            ?: HighlightDisplayLevel.WEAK_WARNING.icon

  override val description: String
    get() = info?.description ?: "Invalid"

  override val severity: Int
    get() = info?.severity?.myVal ?: -1

  override val offset: Int
    get() = info?.actualStartOffset ?: -1

  override fun hashCode() = highlighter.hashCode()

  override fun equals(other: Any?) = other is HighlightingProblem && other.highlighter == highlighter
}
