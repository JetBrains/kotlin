// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ml.common

import com.intellij.codeInsight.completion.ml.CompletionEnvironment
import com.intellij.codeInsight.completion.ml.ContextFeatureProvider
import com.intellij.codeInsight.completion.ml.MLFeatureValue
import com.intellij.completion.ngram.NGram
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.stats.completion.prefix

class CommonLocationFeatures : ContextFeatureProvider {
  override fun getName(): String = "common"
  override fun calculateFeatures(environment: CompletionEnvironment): Map<String, MLFeatureValue> {
    val lookup = environment.lookup
    val editor = lookup.topLevelEditor
    val caretOffset = lookup.lookupStart
    val logicalPosition = editor.offsetToLogicalPosition(caretOffset)
    val lineStartOffset = editor.document.getLineStartOffset(logicalPosition.line)
    val position = environment.parameters.position
    val prefixLength = lookup.prefix().length
    val linePrefix = editor.document.getText(TextRange(lineStartOffset, caretOffset))

    putNGramScorers(environment)
    val prefixToDrop = if (prefixLength > 0) prefixLength else 0
    putContextSimilarityScorers(linePrefix.dropLast(prefixToDrop), position, environment)

    val result = mutableMapOf(
      "line_num" to MLFeatureValue.float(logicalPosition.line),
      "col_num" to MLFeatureValue.float(logicalPosition.column),
      "indent_level" to MLFeatureValue.float(LocationFeaturesUtil.indentLevel(linePrefix, EditorUtil.getTabSize(editor))),
      "is_in_line_beginning" to MLFeatureValue.binary(StringUtil.isEmptyOrSpaces(linePrefix))
    )

    if (DumbService.isDumb(lookup.project)) {
      result["dumb_mode"] = MLFeatureValue.binary(true)
    }

    result["is_after_dot"] = MLFeatureValue.binary(isAfterDot(position))

    result.addPsiParents(position, 10)
    return result
  }

  private fun putNGramScorers(environment: CompletionEnvironment) {
    for ((key, scorer) in NGram.getScorers(environment.parameters, 4))
      environment.putUserData(key, scorer)
  }

  private fun putContextSimilarityScorers(line: String, position: PsiElement, environment: CompletionEnvironment) {
    environment.putUserData(ContextSimilarityUtil.LINE_SIMILARITY_SCORER_KEY,
                            ContextSimilarityUtil.createLineSimilarityScoringFunction(line))
    environment.putUserData(ContextSimilarityUtil.PARENT_SIMILARITY_SCORER_KEY,
                            ContextSimilarityUtil.createParentSimilarityScoringFunction(position))
  }

  private fun MutableMap<String, MLFeatureValue>.addPsiParents(position: PsiElement, numParents: Int) {
    // First parent is always referenceExpression
    var curParent: PsiElement? = position.parent ?: return
    for (i in 1..numParents) {
      curParent = curParent?.parent ?: return
      val parentName = "parent_$i"
      this[parentName] = MLFeatureValue.className(curParent::class.java)
      if (curParent is PsiFileSystemItem) return
    }
  }

  private fun isAfterDot(position: PsiElement) : Boolean {
    val prev = PsiTreeUtil.prevVisibleLeaf(position)
    return prev != null && prev.text == "."
  }
}