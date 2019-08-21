// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.completion.ngram.Ngram.Companion.getNgramPrefix
import com.intellij.completion.ngram.NgramFileConfigurator.Companion.getModelRunner
import com.intellij.stats.completion.CompletionUtil

class NgramLookupListener : LookupListener {

  override fun beforeItemSelected(event: LookupEvent): Boolean {
    val parameters = CompletionUtil.getCurrentCompletionParameters() ?: return true
    val element = event.item ?: return true
    val modelRunner = getModelRunner(parameters.originalFile) ?: return true
    modelRunner.learnTokens(listOf(
      *getNgramPrefix(parameters, modelRunner.getOrder()),
      element.lookupString)
    )
    return true
  }

}