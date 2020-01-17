// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.*
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Key
import com.intellij.stats.completion.events.CompletionStartedEvent
import junit.framework.TestCase

class MLFeaturesLoggingTest : CompletionLoggingTestBase() {
  fun `test context features logged`() = doTest { startedEvent ->
    val contextFactors = startedEvent.contextFactors
    TestCase.assertTrue(contextFactors.isNotEmpty())
    TestCase.assertEquals("1", contextFactors[contextFactorName("binary")])
    TestCase.assertEquals("1.0", contextFactors[contextFactorName("float")])
    TestCase.assertEquals("VALUE1", contextFactors[contextFactorName("categorical")])
  }

  fun `test element features logged`() = doTest { startedEvent ->
    val firstItem = startedEvent.newCompletionListItems[0]
    val relevance = firstItem.relevance!!
    TestCase.assertEquals("0", relevance[elementFactorName("binary")])
    TestCase.assertEquals("2.0", relevance[elementFactorName("float")])
    TestCase.assertEquals("VALUE2", relevance[elementFactorName("categorical")])
  }

  fun `test element features provider can use context features`() = doTest { startedEvent ->
    val firstItem = startedEvent.newCompletionListItems[0]
    val relevance = firstItem.relevance!!
    TestCase.assertEquals("1", relevance[elementFactorName("from_user_data")])
    TestCase.assertEquals("1", relevance[elementFactorName("can_use_context_feature")])
  }

  private fun doTest(checkResults: (CompletionStartedEvent) -> Unit) {
    ContextFeatureProvider.EP_NAME.addExplicitExtension(JavaLanguage.INSTANCE, TestContextFeatureProvider(), testRootDisposable)
    ElementFeatureProvider.EP_NAME.addExplicitExtension(JavaLanguage.INSTANCE, TestElementFeatureProvider(), testRootDisposable)
    myFixture.completeBasic()
    myFixture.type("r")
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    val startedEvent = trackedEvents.first() as CompletionStartedEvent
    checkResults(startedEvent)
  }


  private class TestContextFeatureProvider() : ContextFeatureProvider {
    companion object {
      val USER_DATA_KEY = Key.create<Int>("TEST_KEY")
      const val USER_DATA_TEST_VALUE = 42
    }

    override fun getName(): String = "test"

    override fun calculateFeatures(environment: CompletionEnvironment): Map<String, MLFeatureValue> {
      environment.putUserData(USER_DATA_KEY, USER_DATA_TEST_VALUE)
      return mapOf("binary" to MLFeatureValue.binary(true),
                   "float" to MLFeatureValue.numerical(1),
                   "categorical" to MLFeatureValue.categorical(Category.VALUE1))
    }
  }

  private enum class Category {
    VALUE1, VALUE2
  }

  private class TestElementFeatureProvider() : ElementFeatureProvider {
    override fun getName(): String = "test"

    override fun calculateFeatures(element: LookupElement,
                                   location: CompletionLocation,
                                   contextFeatures: ContextFeatures): Map<String, MLFeatureValue> {
      val expectedValue = contextFeatures.getUserData(
        TestContextFeatureProvider.USER_DATA_KEY) == TestContextFeatureProvider.USER_DATA_TEST_VALUE
      return mapOf("binary" to MLFeatureValue.binary(false),
                   "float" to MLFeatureValue.numerical(2),
                   "categorical" to MLFeatureValue.categorical(Category.VALUE2),
                   "from_user_data" to MLFeatureValue.binary(expectedValue),
                   "can_use_context_feature" to MLFeatureValue.binary(contextFeatures.binaryValue(contextFactorName("binary")) ?: false)
      )
    }
  }

  companion object {
    private fun contextFactorName(name: String) = "ml_ctx_test_$name"

    private fun elementFactorName(name: String) = "ml_test_$name"
  }
}