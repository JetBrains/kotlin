// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.completion.ngram.NGram.score
import com.intellij.completion.ngram.slp.modeling.ngram.NGramModel
import com.intellij.completion.ngram.slp.modeling.runners.ModelRunner
import com.intellij.testFramework.UsefulTestCase

class NgramModelRunnerTest : UsefulTestCase() {
  private val tokens = "public static void main ( String [] args )".split(" ")

  fun `test counting`() {
    val modelRunner = NGram.createModelRunner(
      "two two three three three nine nine nine nine nine nine nine nine nine".split(" ")
    )
    assertTrue(modelRunner.getTokenCount("two") == 2)
    assertTrue(modelRunner.getTokenCount("three") == 3)
    assertTrue(modelRunner.getTokenCount("nine") == 9)
    assertTrue(modelRunner.getTokenCount("zero") == 0)
  }

  fun `test learn content`() {
    val modelRunner = NGram.createModelRunner(tokens)
    val presentProb = modelRunner.score(listOf("public", "static", "void", "main"))
    val replacedFirstProb = modelRunner.score(listOf("args", "static", "void", "main"))
    val replacedLastProb = modelRunner.score(listOf("public", "static", "void", "args"))
    val notPresentProb = modelRunner.score(listOf("not", "present", "in", "content"))
    assertTrue(notPresentProb == 0.0)
    assertTrue(notPresentProb < replacedFirstProb)
    assertTrue(notPresentProb < replacedLastProb)
    assertTrue(replacedFirstProb < presentProb)
    assertTrue(replacedLastProb < presentProb)
  }

  private fun ModelRunner.getTokenCount(token: String): Int =
    (model as? NGramModel)?.counter?.getCounts(vocabulary.toIndices(listOf(token)))?.first()?.toInt() ?: -1

  private fun ModelRunner.learnTokens(tokens: List<String>) {
    model.learn(vocabulary.toIndices(tokens).filterNotNull())
  }
}