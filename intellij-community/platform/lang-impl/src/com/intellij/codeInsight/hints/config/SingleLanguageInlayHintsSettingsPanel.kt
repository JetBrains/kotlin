// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.config

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hint.EditorFragmentComponent
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.settings.ParameterNameHintsSettings
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.CheckBoxList
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.border.LineBorder

// Note: old parameter hints panel is special cased as it rely on global settings, so it doesn't fit to this model,
// but logically belongs to this settings
internal class SingleLanguageInlayHintsSettingsPanel(
  val project: Project,
  val language: Language,
  private val keyToProvider: Map<SettingsKey<out Any>, ProviderWithSettings<out Any>>,
  private val settingsWrappers: List<SettingsWrapper<out Any>>,
  private val providerTypes: List<HintProviderOption<out Any>>
) : JPanel() {
  private val editorField = createEditor()
  private val hintTypeConfigurableToComponent: MutableMap<ImmediateConfigurable, JComponent> = hashMapOf()
  private var selectedProvider: ProviderWithSettings<out Any>? = keyToProvider.entries.firstOrNull()?.value
  private val providerTypesList = object: CheckBoxList<HintProviderOption<out Any>>() {
    override fun isEnabled(index: Int): Boolean {
      return getItemAt(index)?.isOldParameterHints != true || EditorSettingsExternalizable.getInstance().isShowParameterNameHints
    }
  }
  private val bottomPanel = JPanel()
  private val settings = ServiceManager.getService(InlayHintsSettings::class.java)
  private val immediateConfigurableListener = object : ChangeListener {
    override fun settingsChanged() {
      val provider = selectedProvider
      if (provider == null) {
        updateEditor(null)
      }
      else {
        updateEditor(provider.provider.previewText)
      }
    }
  }

  private val parameterHintsPanel = run {
    val hintsProvider = InlayParameterHintsExtension.forLanguage(language)
    val options = hintsProvider?.supportedOptions ?: return@run null
    ParameterHintsSettingsPanel(language, options, hintsProvider.isBlackListSupported)
  }


  init {
    layout = GridLayout(1, 1)

    val top = JPanel()
    top.layout = GridLayout(1, 2)

    val horizontalSplitter = JBSplitter(false, 0.35f)
    top.add(horizontalSplitter)

    val typesListPane = JBScrollPane(providerTypesList)

    horizontalSplitter.firstComponent = typesListPane
    val providerSettingsPane = JBScrollPane()
    providerSettingsPane.border = null
    horizontalSplitter.secondComponent = withInset(providerSettingsPane)

    initProviderList(providerSettingsPane)

    bottomPanel.layout = BorderLayout()
    bottomPanel.add(createPreviewPanel(), BorderLayout.CENTER)
    val selected = selectedProvider
    if (selected != null) {
      updateWithInlayPanel(providerSettingsPane, selected)
    }
    else {
      updateWithParameterHintsPanel(providerSettingsPane)
    }
    providerTypesList.selectedIndex = 0

    val splitter = JBSplitter(true)
    splitter.firstComponent = top
    splitter.secondComponent = bottomPanel
    add(splitter)
  }

  private fun getComponentFor(configurable: ImmediateConfigurable): JComponent {
    return hintTypeConfigurableToComponent.computeIfAbsent(configurable) { it.createComponent(immediateConfigurableListener) }
  }

  fun createEditor(): EditorTextField {
    val fileType: FileType = language.associatedFileType ?: FileTypes.PLAIN_TEXT
    val editorField = object: EditorTextField(null, project, fileType, false, false) {
      override fun addNotify() {
        super.addNotify()
        // only here the editor is finally initialized
        updateHints()
      }
    }
    val scheme = EditorColorsManager.getInstance().globalScheme
    editorField.font = scheme.getFont(EditorFontType.PLAIN)
    editorField.border = LineBorder(JBColor.border())
    editorField.addSettingsProvider { editor ->
      editor.setVerticalScrollbarVisible(true)
      editor.setHorizontalScrollbarVisible(true)
      editor.setBorder(JBUI.Borders.empty(4))
      with(editor.settings) {
        additionalLinesCount = 2
        isAutoCodeFoldingEnabled = false
        isLineNumbersShown = true
      }
      // Sadly, but we can't use daemon here, because we need specific kind of settings instance here.
      editor.document.addDocumentListener(object: DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          updateHints()
        }
      })
      editor.backgroundColor = EditorFragmentComponent.getBackgroundColor(editor, false)
      editor.setBorder(JBUI.Borders.empty())
      // If editor is created as not viewer, daemon is enabled automatically. But we want to collect hints manually with another settings.
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
      if (psiFile != null) {
        DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, false)
      }
    }
    editorField.setCaretPosition(0)
    return editorField
  }

  private fun createPreviewPanel(): JPanel {
    val previewPanel = JPanel()
    previewPanel.layout = BorderLayout()
    previewPanel.add(editorField, BorderLayout.CENTER)
    return previewPanel
  }
  private fun collectAndDrawHints(editor: Editor, file: PsiFile) {
    val provider = selectedProvider
    // if provider == null, old parameter hints panel is current, no preview
    if (provider == null) return
    val settingsWrapper = settingsWrappers.find { it.providerWithSettings.provider === provider.provider }!!
    val collector = settingsWrapper.providerWithSettings.getCollectorWrapperFor(file, editor, settingsWrapper.language)
                    ?: return
    collector.collectTraversingAndApply(editor, file)
  }

  private fun updateEditor(text: String?) {
    if (text == null) {
      bottomPanel.isVisible = false
    }
    else {
      bottomPanel.isVisible = true
      //if (editorField.isValid) {
      editorField.text = text
      updateHints()
    }
  }

  private fun updateHints() {
    val document = editorField.document
    ApplicationManager.getApplication().runWriteAction {
      PsiDocumentManager.getInstance(project).commitDocument(document)
      val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
      val editor = editorField.editor
      if (editor != null && psiFile != null) {
        collectAndDrawHints(editor, psiFile)
      }
    }
  }

  private fun withInset(component: JComponent): JComponent {
    val panel = JPanel(BorderLayout())
    panel.add(component)
    panel.border = JBUI.Borders.empty(2)
    return panel
  }

  fun isModified(): Boolean {
    if (providerTypes.any {
        when {
          it.isOldParameterHints -> it.isEnabled() != isParameterHintsEnabledForLanguage(language)
          else -> it.isEnabled() != settings.hintsEnabled(it.key, language)
        }
      }) return true
    if (parameterHintsPanel?.isModified() == true) return true
    return settingsWrappers
      .any { it.isModified() }
  }

  fun apply() {
    for (settingsWrapper in settingsWrappers) {
      settingsWrapper.apply()
    }
    for (providerType in providerTypes) {
      if (providerType.isOldParameterHints) {
        setShowParameterHintsForLanguage(providerType.isEnabled(), language)
      } else {
        settings.changeHintTypeStatus(providerType.key, language, providerType.isEnabled())
      }
    }
    parameterHintsPanel?.saveOptions()
  }

  private fun initProviderList(typeSettingsPane: JBScrollPane) {
    providerTypesList.setCheckBoxListListener { index, value ->
      providerTypesList.getItemAt(index)?.setEnabled(value)
    }
    providerTypesList.addListSelectionListener {
      val index = providerTypesList.selectedIndex
      val newOption = providerTypesList.getItemAt(index) ?: return@addListSelectionListener
      if (newOption.isOldParameterHints) {
        updateWithParameterHintsPanel(typeSettingsPane)
      } else {
        updateWithInlayPanel(typeSettingsPane, keyToProvider.getValue(newOption.key))
      }
    }
    for (option in providerTypes) {
      providerTypesList.addItem(option, option.name, option.isEnabled())
    }
  }

  private fun updateWithInlayPanel(typeSettingsPane: JBScrollPane,
                                   providerWithSettings: ProviderWithSettings<out Any>) {
    typeSettingsPane.setViewportView(getComponentFor(providerWithSettings.configurable))
    selectedProvider = providerWithSettings
    updateEditor(providerWithSettings.provider.previewText)
  }

  private fun updateWithParameterHintsPanel(typeSettingsPane: JBScrollPane) {
    typeSettingsPane.setViewportView(parameterHintsPanel)
    selectedProvider = null
    updateEditor(null)
  }

  /**
   * Loads state from settings and apply it to UI.
   */
  fun loadFromSettings() {
    SwingUtilities.invokeLater {
      for ((index, providerType) in providerTypes.withIndex()) {
        val enabled = settings.hintsEnabled(providerType.key, language)
        if (!providerType.isOldParameterHints) {
          providerTypesList.setItemSelected(providerType, enabled)
          providerTypesList.getItemAt(index)?.setEnabled(enabled)
        } else {
          providerTypesList.setItemSelected(providerType, ParameterNameHintsSettings.getInstance().isEnabledForLanguage(language))
        }
      }
      providerTypesList.repaint()
    }
  }
}

internal class SettingsWrapper<T : Any>(
  val providerWithSettings: ProviderWithSettings<T>,
  val config: InlayHintsSettings,
  val language: Language
) {
  fun isModified(): Boolean {
    val inSettings = providerWithSettings.settings
    val stored = providerWithSettings.provider.getActualSettings(config, language)
    return inSettings != stored
  }

  fun apply() {
    val provider = providerWithSettings.provider
    val settingsCopy = copySettings(providerWithSettings.settings, provider)
    config.storeSettings(provider.key, language, settingsCopy)
  }
}

