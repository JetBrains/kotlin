// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.internal.ml.DecisionFunction
import com.intellij.internal.ml.FeatureMapper
import com.intellij.internal.ml.completion.RankingModelProvider
import com.intellij.lang.Language
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightPlatformTestCase.assertThrows
import com.jetbrains.completion.ranker.WeakModelProvider
import junit.framework.TestCase

class RankingProvidersTest : LightPlatformTestCase() {
  private lateinit var testLanguage: Language

  fun `test no providers registered`() {
    checkActiveProvider(null)
  }

  fun `test disabled weak provider`() {
    registerProviders(weak(false, false))
    checkActiveProvider(null)
  }

  fun `test replacing weak provider cannot be used`() {
    registerProviders(weak(false, true))
    checkActiveProvider(null)
  }

  fun `test enabled weak provider`() {
    val expectedProvider = weak(true, false)
    registerProviders(expectedProvider)
    checkActiveProvider(expectedProvider)
  }

  fun `test weak provider replace strong`() {
    val expectedProvider = weak(true, true)
    registerProviders(expectedProvider, strong())
    checkActiveProvider(expectedProvider)
  }

  fun `test weak provider should not replace strong`() {
    val expectedProvider = strong()
    registerProviders(weak(true, false), expectedProvider)
    checkActiveProvider(expectedProvider)
  }

  fun `test strong provider used if there is no weak`() {
    val expectedProvider = strong()
    registerProviders(expectedProvider)
    checkActiveProvider(expectedProvider)
  }

  fun `test few weak providers`() {
    val expectedProvider = weak(true, false)
    registerProviders(expectedProvider, weak(false, true), weak(false, false))
    checkActiveProvider(expectedProvider)
  }

  fun `test too many weak providers`() {
    registerProviders(weak(true, false), weak(true, false))
    assertThrows<IllegalStateException>(IllegalStateException::class.java) { WeakModelProvider.findProvider(testLanguage) }
  }

  fun `test too many strong providers`() {
    registerProviders(strong(), strong())
    assertThrows<IllegalStateException>(IllegalStateException::class.java) { WeakModelProvider.findProvider(testLanguage) }
  }

  private fun checkActiveProvider(expectedProvider: RankingModelProvider?) {
    val languageSupported = expectedProvider != null
    TestCase.assertEquals(languageSupported, testLanguage.displayName in RankingSupport.availableLanguages())
    val actualProvider = WeakModelProvider.findProvider(testLanguage)
    TestCase.assertEquals(expectedProvider, actualProvider)
  }

  private fun registerProviders(vararg providers: RankingModelProvider) {
    providers.forEach { WeakModelProvider.registerProvider(it, testRootDisposable) }
  }

  override fun setUp() {
    super.setUp()
    testLanguage = TestLanguage()
  }

  override fun tearDown() {
    try {
      Language.unregisterLanguage(testLanguage)
    }
    finally {
      super.tearDown()
    }
  }

  private fun weak(canBeUsed: Boolean, shouldReplace: Boolean): RankingModelProvider = TestWeakProvider(canBeUsed, shouldReplace,
                                                                                                        testLanguage)

  private fun strong(): RankingModelProvider = TestModelProvider(testLanguage)

  private open class TestModelProvider(private val supportedLanguage: Language) : RankingModelProvider {
    override fun getModel(): DecisionFunction = TestDummyDecisionFunction()

    override fun getDisplayNameInSettings(): String = supportedLanguage.displayName

    override fun isLanguageSupported(language: Language): Boolean = language == supportedLanguage
  }

  private class TestWeakProvider(private val canBeUsed: Boolean, private val shouldReplace: Boolean, supportedLanguage: Language)
    : TestModelProvider(supportedLanguage), WeakModelProvider {
    override fun shouldReplace(): Boolean = shouldReplace
    override fun canBeUsed(): Boolean = canBeUsed
  }

  private class TestDummyDecisionFunction : DecisionFunction {
    override fun getFeaturesOrder(): Array<FeatureMapper> = emptyArray()
    override fun getRequiredFeatures(): List<String> = emptyList()
    override fun getUnknownFeatures(features: MutableCollection<String>): List<String> = emptyList()
    override fun version(): String? = null
    override fun predict(features: DoubleArray?): Double = 0.0
  }

  private class TestLanguage : Language("RankingProvidersTest_TEST_LANG_ID")
}