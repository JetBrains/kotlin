// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.ngram

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.stats.completion.CompletionLoggingTestBase
import com.intellij.stats.completion.events.CompletionStartedEvent
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase

class NGramFeatureLoggedTest : CompletionLoggingTestBase() {
  fun `test ngram is in logs`() {
    myFixture.configureByText(JavaFileType.INSTANCE, "class T { void r() { } public static void main(String[] args) { new T().<caret> } }")
    myFixture.completeBasic()
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    val startedEvent = trackedEvents.first() as CompletionStartedEvent
    UsefulTestCase.assertNotEmpty(startedEvent.newCompletionListItems)
    TestCase.assertTrue(startedEvent.newCompletionListItems.any { it.relevance?.contains("ml_ngram_file") ?: false })
  }
}