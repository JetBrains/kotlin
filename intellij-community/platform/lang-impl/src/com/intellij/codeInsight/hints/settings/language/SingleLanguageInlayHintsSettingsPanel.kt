// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.language

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hint.EditorFragmentComponent
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.settings.InlayHintsConfigurable
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.ide.CopyProvider
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.panel.ComponentPanelBuilder
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.border.LineBorder

private const val TOP_PANEL_PROPORTION = 0.35f

class SingleLanguageInlayHintsSettingsPanel(
  private val myModels: Array<InlayProviderSettingsModel>,
  private val myLanguage: Language,
  private val myProject: Project
) : JPanel(), CopyProvider {
  private val config = InlayHintsSettings.instance()
  private val myProviderList = createList()
  private var myCurrentProvider = selectLastViewedProvider()
  private val myEditorTextField = createEditor()
  private val myCurrentProviderCustomSettingsPane = JBScrollPane().also {
    it.border = null
  }
  private val myCurrentProviderCasesPane = JBScrollPane().also {
    it.border = null
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  }
  private val myBottomPanel = createBottomPanel()
  private var myCasesPanel: CasesPanel? = null
  private val myRightPanel: JPanel = JPanel()
  private val myWarningContainer = JPanel().also {
    it.layout = BoxLayout(it, BoxLayout.Y_AXIS)
  }


  init {
    layout = GridLayout(1, 1)
    val splitter = JBSplitter(true)
    splitter.firstComponent = createTopPanel()
    splitter.secondComponent = myBottomPanel

    for (model in myModels) {
      model.onChangeListener = object : ChangeListener {
        override fun settingsChanged() {
          updateHints()
        }
      }
    }

    myProviderList.addListSelectionListener {
      val newProviderModel = myProviderList.selectedValue
      update(newProviderModel)
      config.saveLastViewedProviderId(newProviderModel.id)
    }
    myProviderList.selectedIndex = findIndexToSelect()

    add(splitter)
    updateWithNewProvider()
  }

  private fun selectLastViewedProvider(): InlayProviderSettingsModel {
    return myModels[findIndexToSelect()]
  }

  private fun findIndexToSelect(): Int {
    val id = config.getLastViewedProviderId() ?: return 0
    return when (val index = myModels.indexOfFirst { it.id == id }) {
      -1 -> 0
      else -> index
    }
  }

  private fun createList(): JBList<InlayProviderSettingsModel> {
    return JBList(*myModels).also {
      it.cellRenderer = object : ColoredListCellRenderer<InlayProviderSettingsModel>(), ListCellRenderer<InlayProviderSettingsModel> {
        override fun customizeCellRenderer(list: JList<out InlayProviderSettingsModel>,
                                           value: InlayProviderSettingsModel,
                                           index: Int,
                                           selected: Boolean,
                                           hasFocus: Boolean) {
          append(value.name)
        }
      }
    }
  }

  private fun createTopPanel(): JPanel {
    val panel = JPanel()
    panel.layout = GridLayout(1, 1)

    val horizontalSplitter = JBSplitter(false, TOP_PANEL_PROPORTION)
    horizontalSplitter.firstComponent = createLeftPanel()
    horizontalSplitter.secondComponent = fillRightPanel()

    panel.add(horizontalSplitter)
    return panel
  }

  private fun createLeftPanel() = JBScrollPane(myProviderList)

  private fun fillRightPanel(): JPanel {
    return withInset(panel {
      row {
        myWarningContainer(growY)
      }
      row {
        withInset(myCurrentProviderCasesPane)()
      }
      row {
        withInset(myCurrentProviderCustomSettingsPane)()
      }
    })
  }

  private fun createBottomPanel(): JPanel {
    val panel = JPanel()
    panel.layout = BorderLayout()
    panel.add(createPreviewPanel(), BorderLayout.CENTER)
    return panel
  }

  private fun createPreviewPanel(): JPanel {
    val previewPanel = JPanel()
    previewPanel.layout = BorderLayout()
    previewPanel.add(myEditorTextField, BorderLayout.CENTER)
    return previewPanel
  }

  private fun createEditor(): EditorTextField {
    val fileType: FileType = myLanguage.associatedFileType ?: FileTypes.PLAIN_TEXT
    val editorField = object : EditorTextField(null, myProject, fileType, false, false) {
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
      editor.document.addDocumentListener(object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
          updateHints()
        }
      })
      editor.backgroundColor = EditorFragmentComponent.getBackgroundColor(editor, false)
      editor.setBorder(JBUI.Borders.empty())
      // If editor is created as not viewer, daemon is enabled automatically. But we want to collect hints manually with another settings.
      val psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.document)
      if (psiFile != null) {
        DaemonCodeAnalyzer.getInstance(myProject).setHighlightingEnabled(psiFile, false)
      }
    }
    editorField.setCaretPosition(0)
    return editorField
  }

  private fun withInset(component: JComponent): JPanel {
    val panel = JPanel(GridLayout())
    panel.add(component)
    panel.border = JBUI.Borders.empty(2)
    return panel
  }

  private fun update(newProvider: InlayProviderSettingsModel) {
    if (myCurrentProvider == newProvider) return
    myCurrentProvider = newProvider
    updateWithNewProvider()
  }

  private fun updateWithNewProvider() {
    myCurrentProviderCasesPane.setViewportView(createCasesPanel())
    myCurrentProviderCustomSettingsPane.setViewportView(myCurrentProvider.component)
    myRightPanel.validate()
    updateWarningPanel()
    val previewText = myCurrentProvider.previewText
    if (previewText == null) {
      myBottomPanel.isVisible = false
    }
    else {
      myBottomPanel.isVisible = true
      myEditorTextField.text = previewText
      updateHints()
    }
  }

  private fun updateWarningPanel() {
    myWarningContainer.removeAll()
    if (!config.hintsEnabled(myLanguage)) {
      myWarningContainer.add(JLabel("Inlay hints for ${myLanguage.displayName} are disabled."))
      myWarningContainer.add(LinkLabel.create("Configure settings.") {
        val settings = Settings.KEY.getData(DataManager.getInstance().getDataContext(this))
        if (settings != null) {
          val mainConfigurable = settings.find(InlayHintsConfigurable::class.java)
          if (mainConfigurable != null) {
            settings.select(mainConfigurable)
          }
        }
      })
    }
    myWarningContainer.revalidate()
    myWarningContainer.repaint()
  }

  private fun createCasesPanel(): JPanel {
    val model = myCurrentProvider
    val casesPanel = CasesPanel(
      cases = model.cases,
      mainCheckBoxName = model.mainCheckBoxLabel,
      loadMainCheckBoxValue = { model.isEnabled },
      onUserChangedMainCheckBox = { model.isEnabled = it },
      listener = model.onChangeListener!!, // must be installed at this point
      disabledExternally = { !(config.hintsEnabled(myLanguage) && config.hintsEnabledGlobally()) }
    )
    myCasesPanel = casesPanel
    return casesPanel
  }

  private fun updateHints() {
    if (myBottomPanel.isVisible) {
      val document = myEditorTextField.document
      ApplicationManager.getApplication().runWriteAction {
        PsiDocumentManager.getInstance(myProject).commitDocument(document)
        val psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document)
        val editor = myEditorTextField.editor
        if (editor != null && psiFile != null) {
          myCurrentProvider.collectAndApply(editor, psiFile)
        }
      }
    }
  }

  fun isModified(): Boolean {
    return myModels.any { it.isModified() }
  }

  fun apply() {
    for (model in myModels) {
      model.apply()
    }
  }

  fun reset() {
    for (model in myModels) {
      model.reset()
    }
    myCasesPanel?.updateFromSettings()
    updateWarningPanel()
  }

  override fun performCopy(dataContext: DataContext) {
    val selectedIndex = myProviderList.selectedIndex
    if (selectedIndex < 0) return
    val selection = myProviderList.model.getElementAt(selectedIndex)
    CopyPasteManager.getInstance().setContents(StringSelection(selection.name))
  }

  override fun isCopyEnabled(dataContext: DataContext): Boolean = !myProviderList.isSelectionEmpty

  override fun isCopyVisible(dataContext: DataContext): Boolean = false
}