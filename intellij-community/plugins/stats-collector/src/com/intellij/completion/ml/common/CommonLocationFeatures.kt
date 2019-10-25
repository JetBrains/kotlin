// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.ml.CompletionEnvironment
import com.intellij.codeInsight.completion.ml.ContextFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.completion.ngram.NGram
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil

class CommonLocationFeatures : ContextFeatureProvider {
  override fun getName(): String = "common"
  override fun calculateFeatures(environment: CompletionEnvironment): Map<String, MLFeatureValue> {
    val lookup = environment.lookup
    val editor = lookup.topLevelEditor
    val caretOffset = lookup.lookupStart
    val logicalPosition = editor.offsetToLogicalPosition(caretOffset)
    val lineStartOffset = editor.document.getLineStartOffset(logicalPosition.line)
    val linePrefix = editor.document.getText(TextRange(lineStartOffset, caretOffset))

    putNGramScorer(environment)

    return mapOf(
      "line_num" to MLFeatureValue.float(logicalPosition.line),
      "col_num" to MLFeatureValue.float(logicalPosition.column),
      "indent_level" to MLFeatureValue.float(LocationFeaturesUtil.indentLevel(linePrefix, EditorUtil.getTabSize(editor))),
      "is_in_line_beginning" to MLFeatureValue.binary(StringUtil.isEmptyOrSpaces(linePrefix))
    )
  }

  private fun putNGramScorer(environment: CompletionEnvironment) {
    val scoringFunction = NGram.createScoringFunction(environment.parameters, 4)
    if(scoringFunction != null) {
      environment.putUserData(NGram.NGRAM_SCORER_KEY, scoringFunction)
    }
  }
}