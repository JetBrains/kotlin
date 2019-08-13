/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jdom.Element
import org.jdom.input.SAXBuilder
import java.io.StringReader


class MockInlayProvider(private val defaultBlackList: Set<String>): InlayParameterHintsProvider {
  override fun getParameterHints(element: PsiElement) = emptyList<InlayInfo>()
  override fun getHintInfo(element: PsiElement) = null
  override fun getDefaultBlackList() = defaultBlackList
}


class ParameterNameSettingsTest : TestCase() {

  lateinit var settings: ParameterNameHintsSettings
  lateinit var inlayProvider: InlayParameterHintsProvider
  
  override fun setUp() {
    settings = ParameterNameHintsSettings()
    inlayProvider = MockInlayProvider(setOf())
  }
  
  fun defaultSettingsUpdated(vararg newDefault: String) {
    inlayProvider = MockInlayProvider(setOf(*newDefault))
  }

  fun addIgnorePattern(newPattern: String) {
    settings.addIgnorePattern(PlainTextLanguage.INSTANCE, newPattern)
  }

  fun setIgnorePattern(vararg newPatternSet: String) {
    val base = inlayProvider.defaultBlackList
    val diff = Diff.build(base, setOf(*newPatternSet))
    
    settings.setBlackListDiff(PlainTextLanguage.INSTANCE, diff)
  }
  
  fun getIgnoreSet(): Set<String> {
    val diff = settings.getBlackListDiff(PlainTextLanguage.INSTANCE)
    return diff.applyOn(inlayProvider.defaultBlackList)
  }

  fun `test ignore pattern is added`() {
    defaultSettingsUpdated("xxx")
    
    var ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 1)
    
    addIgnorePattern("aaa")
    
    ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 2)
    assert(ignoreSet.contains("aaa"))
    assert(ignoreSet.contains("xxx"))
  }

  fun `test if empty element is passed settings are dropped`() {
    addIgnorePattern("new_ignore_pattern")

    var ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 1)

    settings.loadState(Element("element"))

    ignoreSet = getIgnoreSet()
    assert(ignoreSet.isEmpty())
  }

  fun `test removed pattern is removed when defaults are updated`() {
    defaultSettingsUpdated("aaa", "bbb")

    var ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 2)

    setIgnorePattern("aaa")
    assert(getIgnoreSet().size == 1)
    
    defaultSettingsUpdated("aaa", "bbb", "ccc")
    
    ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 2)
    assert(ignoreSet.contains("aaa"))
    assert(ignoreSet.contains("ccc"))
  }

  fun `test added items remain added on defaults update`() {
    defaultSettingsUpdated("aaa")
    var ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 1)

    addIgnorePattern("xxx")
    ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 2)

    defaultSettingsUpdated("aaa", "bbb")
    ignoreSet = getIgnoreSet()
    assert(ignoreSet.size == 3)
    assert(ignoreSet.contains("aaa"))
    assert(ignoreSet.contains("bbb"))
    assert(ignoreSet.contains("xxx"))
  }

  fun `test state is preserved between restarts`() {
    val added = setOf("added")
    val removed = setOf("removed")
    
    settings.setBlackListDiff(PlainTextLanguage.INSTANCE, Diff(added, removed))
    
    val state = settings.state
    settings.loadState(state)

    val diff = settings.getBlackListDiff(PlainTextLanguage.INSTANCE)
    assert(diff.added.contains("added"))
    assert(diff.added.size == 1)
    
    assert(diff.removed.contains("removed"))
    assert(diff.removed.size == 1)
  }

  fun `test state is correctly loaded from incorrect model`() {
    val text = """
<settings>
  <blacklists>
    <blacklist language="Plain text">
      <removed pattern="removed" />
    </blacklist>
    <blacklist language="Plain text">
      <added pattern="added" />
    </blacklist>
  </blacklists>
</settings>
"""
    
    val root = SAXBuilder().build(StringReader(text)).rootElement
    settings.loadState(root)
    
    val diff = settings.getBlackListDiff(PlainTextLanguage.INSTANCE)
    assert(diff.added.contains("added"))
    assert(diff.added.size == 1)
    
    assert(diff.removed.contains("removed"))
    assert(diff.removed.size == 1)
  }
  
}