// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.completion.tracker.setupCompletionContext
import com.intellij.internal.ml.DecisionFunction
import com.intellij.internal.ml.FeatureMapper
import com.intellij.internal.ml.FloatFeature
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.castSafelyTo
import com.jetbrains.completion.ranker.WeakModelProvider
import junit.framework.TestCase
import java.beans.PropertyChangeListener

class ChangeArrowsDrawingTest : LightFixtureCompletionTestCase() {
  private var arrowChecker = ArrowPresenceChecker()
  override fun setUp() {
    super.setUp()
    val settings = CompletionMLRankingSettings.getInstance()
    val settingsStateBefore = MLRankingSettingsState.build("Java", settings)
    with(settings) {
      isRankingEnabled = true
      isShowDiffEnabled = true
      setLanguageEnabled("Java", true)
    }

    RankingSupport.enableInTests(testRootDisposable)
    Disposer.register(testRootDisposable, Disposable { settingsStateBefore.restore(CompletionMLRankingSettings.getInstance()) })
    LookupManager.getInstance(project)
      .addPropertyChangeListener(
        PropertyChangeListener { evt -> evt.newValue?.castSafelyTo<LookupImpl>()?.addPresentationCustomizer(arrowChecker) },
        testRootDisposable)
    WeakModelProvider.registerProvider(TestInverseRankingModelProvider(), testRootDisposable)
  }

  fun testArrowsAreShowingAndUpdated() {
    setupCompletionContext(myFixture)
    complete()
    val invokedCountBeforeTyping = arrowChecker.invokedCount
    type('r')
    arrowChecker.assertArrowsAvailable()
    TestCase.assertTrue(invokedCountBeforeTyping < arrowChecker.invokedCount)
  }

  private class ArrowPresenceChecker : LookupCellRenderer.ItemPresentationCustomizer {
    var invokedCount: Int = 0
    private var arrowsFound: Boolean = false
    private var diffKeyFound = false
    override fun customizePresentation(item: LookupElement, presentation: LookupElementPresentation): LookupElementPresentation {
      invokedCount += 1
      diffKeyFound = diffKeyFound || (item.getUserData(PositionDiffArrowInitializer.POSITION_DIFF_KEY) != null)
      val tailFragments = presentation.tailFragments
      if (tailFragments.isNotEmpty()) {
        val textAfterLookupString = tailFragments[0].text.trim()
        arrowsFound = arrowsFound || (textAfterLookupString.startsWith("↑") || textAfterLookupString.startsWith("↓"))
      }

      return presentation
    }

    fun assertArrowsAvailable() {
      TestCase.assertTrue(invokedCount > 1)
      TestCase.assertTrue(arrowsFound)
      TestCase.assertTrue(diffKeyFound)
    }
  }

  private class TestInverseRankingModelProvider : WeakModelProvider {
    override fun canBeUsed(): Boolean = true

    override fun shouldReplace(): Boolean = true

    override fun getModel(): DecisionFunction {
      return object : DecisionFunction {
        override fun getFeaturesOrder(): Array<FeatureMapper> {
          return arrayOf(FloatFeature("position", 1000.0, false).createMapper(null))
        }

        override fun getRequiredFeatures(): List<String> = listOf("position")

        override fun getUnknownFeatures(features: MutableCollection<String>): List<String> = emptyList()

        override fun version(): String? = null

        override fun predict(features: DoubleArray): Double = features[0]
      }
    }

    override fun getDisplayNameInSettings(): String = "Test provider"

    override fun isLanguageSupported(language: Language): Boolean = true
  }

  private data class MLRankingSettingsState(val language: String,
                                            val diffEnabled: Boolean,
                                            val languageEnabled: Boolean,
                                            val rankingEnabled: Boolean) {
    companion object {
      fun build(language: String, settings: CompletionMLRankingSettings): MLRankingSettingsState {
        return MLRankingSettingsState(language, settings.isRankingEnabled, settings.isShowDiffEnabled, settings.isLanguageEnabled(language))
      }
    }

    fun restore(settings: CompletionMLRankingSettings) {
      with(settings) {
        isRankingEnabled = rankingEnabled
        isShowDiffEnabled = diffEnabled
        setLanguageEnabled(language, languageEnabled)
      }
    }
  }
}