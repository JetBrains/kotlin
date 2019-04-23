// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.filtering.MatcherConstructor
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.text.StringUtil
import java.util.*
import java.util.stream.Collectors


fun getHintProviders(): List<Pair<Language, InlayParameterHintsProvider>> {
  val name = ExtensionPointName<LanguageExtensionPoint<InlayParameterHintsProvider>>("com.intellij.codeInsight.parameterNameHints")
  val languages = name.extensionList.map { it.language }
  return languages
    .mapNotNull { Language.findLanguageByID(it) }
    .map { it to InlayParameterHintsExtension.forLanguage(it) }
}


fun getBlackListInvalidLineNumbers(text: String): List<Int> {
  val rules = StringUtil.split(text, "\n", true, false)
  return rules
    .asSequence()
    .mapIndexedNotNull { index, s -> index to s }
    .filter { it.second.isNotEmpty() }
    .map { it.first to MatcherConstructor.createMatcher(it.second) }
    .filter { it.second == null }
    .map { it.first }
    .toList()
}

fun getLanguageForSettingKey(language: Language): Language {
  val supportedLanguages = getBaseLanguagesWithProviders()
  var languageForSettings: Language? = language
  while (languageForSettings != null && !supportedLanguages.contains(languageForSettings)) {
    languageForSettings = languageForSettings.baseLanguage
  }
  if (languageForSettings == null) languageForSettings = language
  return languageForSettings
}

fun getBaseLanguagesWithProviders(): List<Language> {
  return getHintProviders()
    .stream()
    .map { (first) -> first }
    .sorted(Comparator.comparing<Language, String> { l -> l.displayName })
    .collect(Collectors.toList())
}