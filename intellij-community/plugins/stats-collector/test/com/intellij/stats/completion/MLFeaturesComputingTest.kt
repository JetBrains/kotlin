// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.completion.CompletionLocation
import com.intellij.codeInsight.completion.ml.*
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.completion.sorting.RankingSupport
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.extensions.Extensions
import com.intellij.stats.storage.factors.MutableLookupStorage
import junit.framework.TestCase

class MLFeaturesComputingTest : CompletionLoggingTestBase() {
  fun `test features should be calculated if logging enabled`() = doTest(true, false, true)

  fun `test features should be calculated if ranking enabled`() = doTest(false, true, true)

  fun `test features should be calculated if ranking and logging enabled`() = doTest(true, true, true)

  fun `test features should not be calculated if ranking and logging disabled`() = doTest(false, false, false)

  private fun doTest(enableLogging: Boolean, enableRanking: Boolean, shouldCompute: Boolean) {
    val contextFeatureProvider = TestContextFeatureProvider()
    ContextFeatureProvider.EP_NAME.addExplicitExtension(JavaLanguage.INSTANCE, contextFeatureProvider, testRootDisposable)
    val elementFeatureProvider = TestElementFeatureProvider()
    ElementFeatureProvider.EP_NAME.addExplicitExtension(JavaLanguage.INSTANCE, elementFeatureProvider, testRootDisposable)

    MutableLookupStorage.setComputeFeaturesAlways(false, testRootDisposable)

    setLoggingEnabled(enableLogging)
    setRankingEnabled(enableRanking)

    myFixture.completeBasic()
    myFixture.type("r")
    myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)

    TestCase.assertEquals(shouldCompute, contextFeatureProvider.invocationCount != 0)
    if (shouldCompute) {
      TestCase.assertEquals("Context features should be calculated exactly once", 1, contextFeatureProvider.invocationCount)
    }
    TestCase.assertEquals(shouldCompute, elementFeatureProvider.invocationCount != 0)
  }

  private fun setLoggingEnabled(value: Boolean) {
    if (!value) {
      Extensions.getRootArea().getExtensionPoint(CompletionTrackerDisabler.EpName).registerExtension(object : CompletionTrackerDisabler {
        override fun isDisabled(): Boolean = true
      }, testRootDisposable)
    }
  }

  private fun setRankingEnabled(value: Boolean) {
    if (value) {
      RankingSupport.enableInTests(testRootDisposable)
    }
  }

  private class TestContextFeatureProvider : ContextFeatureProvider {
    @Volatile
    var invocationCount = 0

    override fun getName(): String = "test"

    override fun calculateFeatures(environment: CompletionEnvironment): Map<String, MLFeatureValue> {
      invocationCount += 1
      return emptyMap()
    }
  }

  private class TestElementFeatureProvider() : ElementFeatureProvider {
    override fun getName(): String = "test"

    @Volatile
    var invocationCount = 0

    override fun calculateFeatures(element: LookupElement,
                                   location: CompletionLocation,
                                   contextFeatures: ContextFeatures): Map<String, MLFeatureValue> {
      invocationCount += 1
      return emptyMap()
    }
  }
}
