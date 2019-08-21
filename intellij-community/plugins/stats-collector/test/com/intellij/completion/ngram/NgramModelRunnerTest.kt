// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.testFramework.UsefulTestCase

class NgramModelRunnerTest : UsefulTestCase() {
  private val tokens = "public static void main ( String [] args )".split(" ")

  fun `test counting`() {
    val modelRunner = makeModelRunner()
    modelRunner.learnTokens("two two three three three nine nine nine nine nine nine nine nine nine".split(" "))
    assertTrue(modelRunner.getTokenCount("two") == 2)
    assertTrue(modelRunner.getTokenCount("three") == 3)
    assertTrue(modelRunner.getTokenCount("nine") == 9)
    assertTrue(modelRunner.getTokenCount("zero") == 0)
  }

  fun `test learn content`() {
    val modelRunner = makeModelRunner()
    modelRunner.learnTokens(tokens)
    val presentProb = modelRunner.getNgramProbability(Ngram(listOf("public", "static", "void", "main"), 4))
    val replacedFirstProb = modelRunner.getNgramProbability(Ngram(listOf("args", "static", "void", "main"), 4))
    val replacedLastProb = modelRunner.getNgramProbability(Ngram(listOf("public", "static", "void", "args"), 4))
    val notPresentProb = modelRunner.getNgramProbability(Ngram(listOf("not", "present", "in", "content"), 4))
    assertTrue(notPresentProb == 0.0)
    assertTrue(notPresentProb < replacedFirstProb)
    assertTrue(notPresentProb < replacedLastProb)
    assertTrue(replacedFirstProb < presentProb)
    assertTrue(replacedLastProb < presentProb)
  }

  fun `test selfTesting functionality`() {
    val modelRunner = makeModelRunner()
    modelRunner.learnTokens(tokens)
    val probWithoutForgetting = modelRunner.getNgramProbability(Ngram(listOf("public", "static", "void", "main"), 4))
    modelRunner.setSelfTesting(true)
    val probWithForgetting = modelRunner.getNgramProbability(Ngram(listOf("public", "static", "void", "main"), 4))
    assertTrue(probWithForgetting == 0.0)
    assertTrue(probWithForgetting < probWithoutForgetting)
  }

}