// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.ml.BinaryValue
import com.intellij.completion.ml.ContextFeatureProvider
import com.intellij.completion.ml.FloatValue
import com.intellij.completion.ml.MLFeatureValue
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil

class CommonLocationFeatures : ContextFeatureProvider {
  override fun getName(): String = "common"

  override fun calculateFeatures(lookup: LookupImpl): Map<String, MLFeatureValue> {
    val editor = lookup.editor
    val caretOffset = lookup.lookupOriginalStart
    val logicalPosition = editor.offsetToLogicalPosition(caretOffset)
    val lineStartOffset = editor.document.getLineStartOffset(logicalPosition.line)
    val linePrefix = editor.document.getText(TextRange(lineStartOffset, caretOffset))

    return mapOf(
      "line_num" to FloatValue.of(logicalPosition.line),
      "col_num" to FloatValue.of(logicalPosition.column),
      "is_in_line_beginning" to BinaryValue.of(StringUtil.isEmptyOrSpaces(linePrefix)),
      "indent_level" to FloatValue.of(LocationFeaturesUtil.indentLevel(linePrefix, EditorUtil.getTabSize(editor)))
    )
  }
}