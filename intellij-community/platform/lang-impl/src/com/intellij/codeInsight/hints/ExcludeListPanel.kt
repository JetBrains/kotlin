// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.hints.settings.Diff
import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.CodeInsightColors.ERRORS_ATTRIBUTES
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.layout.*
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel


class ExcludeListDialog(val language: Language, private val patternToAdd: String? = null) : DialogWrapper(null) {
  lateinit var myEditor: EditorTextField
  private var myPatternsAreValid = true

  init {
    title = CodeInsightBundle.message("settings.inlay.parameter.hints.exclude.list")
    init()
  }

  override fun createCenterPanel(): JComponent? {
    return createExcludePanel(language)
  }


  private fun createExcludePanel(language: Language): JPanel? {
    val provider = InlayParameterHintsExtension.forLanguage(language)
    if (!provider.isBlackListSupported) return null

    val blackList = getLanguageExcludeList(language)
    val finalText = if (patternToAdd != null) {
      blackList + "\n" + patternToAdd
    }
    else {
      blackList
    }
    val editorTextField = createExcludeListEditorField(finalText)
    editorTextField.alignmentX = Component.LEFT_ALIGNMENT
    editorTextField.addDocumentListener(object : DocumentListener {
      override fun documentChanged(e: DocumentEvent) {
        updateOkEnabled(editorTextField)
      }
    })
    updateOkEnabled(editorTextField)

    myEditor = editorTextField


    return panel {
      row {
        row {
          right {
            link("Reset") {
              setLanguageBlacklistToDefault(language)
            }
          }
        }

        row {
          editorTextField(grow)
        }
        row {
          baseLanguageComment(provider)?.also {
            commentRow(it)
          }
          commentRow(getBlacklistExplanationHTML(language))
        }
      }
    }
  }

  private fun baseLanguageComment(provider: InlayParameterHintsProvider): String? {
    return provider.blackListDependencyLanguage
      ?.let { CodeInsightBundle.message("inlay.hints.base.blacklist.description", it.displayName) }
  }

  private fun setLanguageBlacklistToDefault(language: Language) {
    val provider = InlayParameterHintsExtension.forLanguage(language)
    val defaultBlacklist = provider!!.defaultBlackList
    myEditor.text = StringUtil.join(defaultBlacklist, "\n")
  }

  private fun updateOkEnabled(editorTextField: EditorTextField) {
    val text = editorTextField.text
    val invalidLines = getExcludeListInvalidLineNumbers(text)
    myPatternsAreValid = invalidLines.isEmpty()

    okAction.isEnabled = myPatternsAreValid

    val editor = editorTextField.editor
    if (editor != null) {
      highlightErrorLines(invalidLines, editor)
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    val excludeList = myEditor.text
    storeExcludeListDiff(language, excludeList)
  }

  private fun storeExcludeListDiff(language: Language, text: String) {
    val updatedBlackList = text.split("\n").filter { e -> e.trim { it <= ' ' }.isNotEmpty() }.toSet()

    val provider = InlayParameterHintsExtension.forLanguage(language)
    val defaultBlackList = provider.defaultBlackList
    val diff = Diff.build(defaultBlackList, updatedBlackList)
    ParameterNameHintsSettings.getInstance().setBlackListDiff(getLanguageForSettingKey(language), diff)
    ParameterHintsPassFactory.forceHintsUpdateOnNextPass()
  }
}


private fun getLanguageExcludeList(language: Language): String {
  val hintsProvider = InlayParameterHintsExtension.forLanguage(language) ?: return ""
  val diff = ParameterNameHintsSettings.getInstance().getBlackListDiff(getLanguageForSettingKey(language))
  val excludeList = diff.applyOn(hintsProvider.defaultBlackList)
  return StringUtil.join(excludeList, "\n")
}

private fun createExcludeListEditorField(text: String): EditorTextField {
  val document = EditorFactory.getInstance().createDocument(text)
  val field = EditorTextField(document, null, FileTypes.PLAIN_TEXT, false, false)
  field.preferredSize = Dimension(400, 350)
  field.addSettingsProvider { editor ->
    editor.setVerticalScrollbarVisible(true)
    editor.setHorizontalScrollbarVisible(true)
    editor.settings.additionalLinesCount = 2
    highlightErrorLines(getExcludeListInvalidLineNumbers(text), editor)
  }
  return field
}

private fun highlightErrorLines(lines: List<Int>, editor: Editor) {
  val document = editor.document
  val totalLines = document.lineCount

  val model = editor.markupModel
  model.removeAllHighlighters()
  lines.stream()
    .filter { current -> current < totalLines }
    .forEach { line -> model.addLineHighlighter(ERRORS_ATTRIBUTES, line!!, HighlighterLayer.ERROR) }
}

private fun getBlacklistExplanationHTML(language: Language): String {
  val hintsProvider = InlayParameterHintsExtension.forLanguage(language) ?: return CodeInsightBundle.message(
    "inlay.hints.blacklist.pattern.explanation")
  return hintsProvider.blacklistExplanationHTML
}