// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.suggested

import com.intellij.codeInsight.completion.LookupElementListPresenter
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution.NewParameterValue
import com.intellij.ui.LanguageTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.util.function.Supplier
import javax.swing.*
import kotlin.math.roundToInt

internal class ChangeSignaturePopup(
  signatureChangeModel: SignatureChangePresentationModel,
  nameOfStuffToUpdate: String,
  newParameterData: List<SuggestedRefactoringUI.NewParameterData>,
  project: Project,
  refactoringSupport: SuggestedRefactoringSupport,
  language: Language,
  colorsScheme: EditorColorsScheme,
  screenSize: Dimension
) : JPanel(BorderLayout()), Disposable {
  private val button = object : JButton() {
    override fun isDefaultButton() = true
  }

  private val updateButtonText = RefactoringBundle.message("suggested.refactoring.update.button.text")
  private val nextButtonText = RefactoringBundle.message("suggested.refactoring.next.button.text")

  private val editorFont = Font(colorsScheme.editorFontName, Font.PLAIN, colorsScheme.editorFontSize)

  private val signatureChangePage = SignatureChangesPage(signatureChangeModel, editorFont, screenSize, nameOfStuffToUpdate)

  private val parameterValuesPage = if (newParameterData.isNotEmpty()) {
    ParameterValuesPage(project, language, refactoringSupport, editorFont, newParameterData, this::onParameterValueChanged).also {
      Disposer.register(this, it)
    }
  }
  else {
    null
  }

  private enum class Page { SignatureChange, ParameterValues }

  private var currentPage = Page.SignatureChange

  init {
    val buttonPanel = JPanel(BorderLayout()).apply {
      add(button, BorderLayout.EAST)
    }

    add(signatureChangePage, BorderLayout.CENTER)
    add(buttonPanel, BorderLayout.SOUTH)

    border = JBUI.Borders.empty(5, 2)

    button.text = if (parameterValuesPage == null) updateButtonText else nextButtonText

    button.addActionListener {
      when (currentPage) {
        Page.SignatureChange -> {
          if (parameterValuesPage != null) {
            parameterValuesPage.initialize()
            remove(signatureChangePage)
            add(parameterValuesPage, BorderLayout.CENTER)
            button.text = updateButtonText
            onParameterValueChanged(null)
            currentPage = Page.ParameterValues
            onNext()
            parameterValuesPage.defaultFocus()
          }
          else {
            onOk(newParameterData.map { NewParameterValue.None })
          }
        }

        Page.ParameterValues -> {
          val values = parameterValuesPage!!.getValues()
          if (values != null) {
            onOk(values)
          }
        }
      }
    }
  }

  lateinit var onNext: () -> Unit
  lateinit var onOk: (newParameterValues: List<NewParameterValue>) -> Unit

  fun onEnter() {
    button.doClick()
  }

  fun isEnterEnabled(): Boolean {
    return when (currentPage) {
      Page.SignatureChange -> true
      Page.ParameterValues -> !parameterValuesPage!!.isLookupShown() && parameterValuesPage.areAllValuesCorrect()
    }
  }

  fun isEscapeEnabled(): Boolean {
    return when (currentPage) {
      Page.SignatureChange -> true
      Page.ParameterValues -> !parameterValuesPage!!.isLookupShown()
    }
  }

  override fun dispose() {}

  private fun onParameterValueChanged(validationInfo: ValidationInfo?) {
    button.isEnabled = (validationInfo == null || validationInfo.okEnabled) &&
                       (parameterValuesPage == null || parameterValuesPage.areAllValuesCorrect())
  }
}

private class SignatureChangesPage(
  signatureChangeModel: SignatureChangePresentationModel,
  editorFont: Font,
  screenSize: Dimension,
  nameOfStuffToUpdate: String
) : JPanel(BorderLayout()) {
  init {
    val presentation = createPresentation(signatureChangeModel, editorFont, screenSize)

    add(JLabel(RefactoringBundle.message("suggested.refactoring.change.signature.label.text", nameOfStuffToUpdate)), BorderLayout.NORTH)
    add(
      object : JComponent() {
        init {
          preferredSize = presentation.requiredSize
        }

        override fun paint(g: Graphics) {
          presentation.paint(g as Graphics2D, Rectangle(0, 0, width, height))
        }
      },
      BorderLayout.CENTER
    )

    border = JBUI.Borders.empty(0, 4)
  }

  private fun createPresentation(
    model: SignatureChangePresentationModel,
    editorFont: Font,
    screenSize: Dimension
  ): SignatureChangePresentation {
    // we use editor scheme that is the default for the current UI theme
    // (we don't use one from the current editor because one can have light UI theme and dark editor scheme or vise versa)
    val themeColorsScheme = EditorColorsManager.getInstance().schemeForCurrentUITheme
    var font = editorFont
    val maxWidthHorizontalMode = screenSize.width * 0.75
    val maxWidth = screenSize.width * 0.9
    val maxHeight = screenSize.height * 0.9
    val minFontSize = 8
    while (true) {
      var presentation = SignatureChangePresentation(model, font, themeColorsScheme, verticalMode = false)
      if (presentation.requiredSize.width > maxWidthHorizontalMode) {
        presentation = SignatureChangePresentation(model, font, themeColorsScheme, verticalMode = true)
      }
      if (presentation.requiredSize.width <= maxWidth && presentation.requiredSize.height <= maxHeight || font.size <= minFontSize) {
        return presentation
      }
      font = Font(font.name, font.style, font.size - 1)
    }
  }
}

private class ParameterValuesPage(
  private val project: Project,
  private val language: Language,
  private val refactoringSupport: SuggestedRefactoringSupport,
  private val editorFont: Font,
  private val newParameterData: List<SuggestedRefactoringUI.NewParameterData>,
  private val onValueChanged: (ValidationInfo?) -> Unit
) : JPanel(BorderLayout()), Disposable {
  private val textFields = mutableListOf<LanguageTextField>()
  private val checkBoxes = mutableListOf<JCheckBox?>()
  private val focusSequence = mutableListOf<() -> JComponent>()

  fun initialize() {
    val label = JLabel(RefactoringBundle.message("suggested.refactoring.parameter.values.label.text")).apply {
      border = JBUI.Borders.empty(0, 6, 0, 6)
    }
    add(label, BorderLayout.NORTH)

    val panel = JPanel(GridBagLayout()).apply {
      border = JBUI.Borders.empty(10, 6, 18, 6)
    }
    add(panel, BorderLayout.CENTER)

    val dummyContext = FontRenderContext(AffineTransform(), false, false)
    val textFieldWidth = (editorFont.getMaxCharBounds(dummyContext).width * 25).roundToInt()

    val c = GridBagConstraints()
    c.gridy = 0
    c.anchor = GridBagConstraints.LINE_START
    c.fill = GridBagConstraints.HORIZONTAL
    var isFirstCheckbox = true
    for (data in newParameterData) {
      c.gridx = 0

      c.weightx = 0.0
      c.insets = Insets(0, 0, 0, 0)
      panel.add(JLabel("${data.presentableName}:").apply { font = editorFont }, c)
      c.gridx++

      c.weightx = 1.0
      c.insets = Insets(0, 4, 0, 0)
      val documentManager = PsiDocumentManager.getInstance(project)
      val document = documentManager.getDocument(data.valueFragment)!!
      val textField = MyTextField(language, project, document, data.placeholderText).apply {
        setPreferredWidth(textFieldWidth)

        ComponentValidator(this@ParameterValuesPage)
          .withValidator(
            Supplier {
              documentManager.commitDocument(document)
              refactoringSupport.ui.validateValue(data, component).also { onValueChanged(it) }
            }
          )
          .installOn(component)
          .andRegisterOnDocumentListener(this)
          .andStartOnFocusLost()
      }
      panel.add(textField, c)
      textFields.add(textField)
      focusSequence.add { textField.editor!!.contentComponent }
      c.gridx++

      if (data.offerToUseAnyVariable) {
        c.weightx = 0.0
        c.insets = Insets(0, 10, 0, 0)
        val checkBox = JCheckBox(RefactoringBundle.message("suggested.refactoring.use.any.variable.checkbox.text"))
        panel.add(checkBox, c)
        checkBoxes.add(checkBox)
        focusSequence.add { checkBox }

        checkBox.addItemListener {
          textField.isEnabled = !checkBox.isSelected
        }

        if (isFirstCheckbox) {
          val hint = RefactoringBundle.message("suggested.refactoring.use.any.variable.checkbox.hint")
          panel.add(UI.PanelFactory.panel(checkBox).withTooltip(hint).createPanel(), c)
        }
        isFirstCheckbox = false
      }
      else {
        checkBoxes.add(null)
      }

      c.gridy++
    }

    isFocusCycleRoot = true
    focusTraversalPolicy = MyFocusTraversalPolicy()
  }

  fun defaultFocus() {
    focusSequence.first()().requestFocusInWindow()
  }

  fun isLookupShown(): Boolean {
    return textFields.any {
      (LookupManager.getActiveLookup(it.editor) as LookupElementListPresenter?)?.isShown ?: false
    }
  }

  fun areAllValuesCorrect(): Boolean {
    return textFields.indices.all { getValue(it) != null }
  }

  fun getValues(): List<NewParameterValue>? {
    return textFields.indices.map { index -> getValue(index) ?: return null }
  }

  override fun dispose() {}

  private fun getValue(index: Int): NewParameterValue? {
    val checkBox = checkBoxes[index]
    if (checkBox != null && checkBox.isSelected) return NewParameterValue.AnyVariable

    val data = newParameterData[index]
    if (refactoringSupport.ui.validateValue(data, null).let { it != null && !it.okEnabled }) return null

    return when {
      textFields[index].text.isBlank() -> NewParameterValue.None
      else -> refactoringSupport.ui.extractValue(data.valueFragment)
    }
  }

  private class MyTextField(language: Language, project: Project, document: Document, private val placeholderText: String?)
    : LanguageTextField(language, project, "", { _, _, _ -> document }, true)
  {
    override fun createEditor(): EditorEx {
      return super.createEditor().apply {
        setPlaceholder(placeholderText ?: RefactoringBundle.message("suggested.refactoring.parameter.values.placeholder"))
        setShowPlaceholderWhenFocused(true)
      }
    }
  }

  private inner class MyFocusTraversalPolicy : FocusTraversalPolicy() {
    override fun getComponentAfter(aContainer: Container, aComponent: Component?): Component? {
      val index = focusSequence.indexOfFirst { it() == aComponent }
      if (index < 0) return null
      return if (index < focusSequence.lastIndex) focusSequence[index + 1]() else focusSequence.first()()
    }

    override fun getComponentBefore(aContainer: Container, aComponent: Component?): Component? {
      val index = focusSequence.indexOfFirst { it() == aComponent }
      if (index < 0) return null
      return if (index > 0) focusSequence[index - 1]() else focusSequence.last()()
    }

    override fun getDefaultComponent(aContainer: Container) = focusSequence.first()()
    override fun getFirstComponent(aContainer: Container) = focusSequence.first()()
    override fun getLastComponent(aContainer: Container) = focusSequence.last()()
  }
}
