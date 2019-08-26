// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("MayBeConstant")

package com.intellij.codeInsight.daemon.impl

import com.intellij.codeInsight.daemon.DaemonBundle
import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider.isShowActions
import com.intellij.codeInsight.daemon.impl.tooltips.TooltipActionProvider.setShowActions
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.codeInsight.hint.LineTooltipRenderer
import com.intellij.codeInsight.hint.TooltipGroup
import com.intellij.icons.AllIcons
import com.intellij.ide.TooltipEvent
import com.intellij.ide.ui.UISettings
import com.intellij.internal.statistic.service.fus.collectors.TooltipActionsLogger
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionMenuItem
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.TooltipAction
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.GraphicsConfig
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.*
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.MenuSelectionManager
import javax.swing.event.HyperlinkEvent


val runActionCustomShortcutSet: CustomShortcutSet = CustomShortcutSet(
  KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK or KeyEvent.ALT_DOWN_MASK))

internal class DaemonTooltipWithActionRenderer(text: String?,
                                               private val tooltipAction: TooltipAction?,
                                               width: Int,
                                               comparable: Array<Any>) : DaemonTooltipRenderer(text, width, comparable) {


  override fun dressDescription(editor: Editor, tooltipText: String, expand: Boolean): String {
    if (!LineTooltipRenderer.isActiveHtml(myText!!) || expand) {
      return super.dressDescription(editor, tooltipText, expand)
    }

    val problems = getProblems(tooltipText)
    val text = StringBuilder()
    StringUtil.join(problems, { param ->
      val ref = getLinkRef(param)
      if (ref != null) {
        getHtmlForProblemWithLink(param!!)
      }
      else {
        UIUtil.getHtmlBody(Html(param).setKeepFont(true))
      }
    }, UIUtil.BORDER_LINE, text)

    return text.toString()
  }

  override fun getHtmlForProblemWithLink(problem: String): String {
    //remove "more... (keymap)" info

    val html = Html(problem).setKeepFont(true)
    val extendMessage = DaemonBundle.message("inspection.extended.description")
    var textToProcess = UIUtil.getHtmlBody(html)
    val indexOfMore = textToProcess.indexOf(extendMessage)
    if (indexOfMore < 0) return textToProcess
    val keymapStartIndex = textToProcess.indexOf("(", indexOfMore)
    if (keymapStartIndex > 0) {
      val keymapEndIndex = textToProcess.indexOf(")", keymapStartIndex)
      if (keymapEndIndex > 0) {
        textToProcess = textToProcess.substring(0, keymapStartIndex) + textToProcess.substring(keymapEndIndex + 1, textToProcess.length)
      }
    }

    return textToProcess.replace(extendMessage, "")
  }

  override fun createHint(editor: Editor,
                          p: Point,
                          alignToRight: Boolean,
                          group: TooltipGroup,
                          hintHint: HintHint,
                          newLayout: Boolean,
                          highlightActions: Boolean,
                          limitWidthToScreen: Boolean,
                          tooltipReloader: TooltipReloader?): LightweightHint {
    return super.createHint(editor, p, alignToRight, group, hintHint, newLayout,
                            highlightActions || !(isShowActions() && tooltipAction != null && hintHint.isAwtTooltip),
                            limitWidthToScreen, tooltipReloader)
  }

  override fun fillPanel(editor: Editor,
                         grid: JPanel,
                         hint: LightweightHint,
                         hintHint: HintHint,
                         actions: MutableList<in AnAction>,
                         tooltipReloader: TooltipReloader,
                         newLayout: Boolean,
                         highlightActions: Boolean) {
    super.fillPanel(editor, grid, hint, hintHint, actions, tooltipReloader, newLayout, highlightActions)
    val hasMore = LineTooltipRenderer.isActiveHtml(myText!!)
    if (tooltipAction == null && !hasMore) return

    val settingsComponent = createSettingsComponent(hintHint, tooltipReloader, hasMore, newLayout)

    val settingsConstraints = GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                                                 JBUI.insets(if (newLayout) 7 else 4, 7, if (newLayout) 0 else 4, if (newLayout) 2 else 4),
                                                 0, 0)
    grid.add(settingsComponent, settingsConstraints)

    if (isShowActions()) {
      addActionsRow(hintHint, hint, editor, actions, grid, newLayout, highlightActions)
    }
  }

  private fun addActionsRow(hintHint: HintHint,
                            hint: LightweightHint,
                            editor: Editor,
                            actions: MutableList<in AnAction>,
                            grid: JComponent,
                            newLayout: Boolean,
                            highlightActions: Boolean) {
    if (tooltipAction == null || !hintHint.isAwtTooltip) return


    val buttons = JPanel(GridBagLayout())
    val wrapper = createActionPanelWithBackground(highlightActions)
    wrapper.add(buttons, BorderLayout.WEST)

    buttons.border = JBUI.Borders.empty()
    buttons.isOpaque = false

    val runFixAction = { event: InputEvent? ->
      hint.hide()
      tooltipAction.execute(editor, event)
    }

    val shortcutRunActionText = KeymapUtil.getShortcutsText(runActionCustomShortcutSet.shortcuts)
    val shortcutShowAllActionsText = getKeymap(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)

    val gridBag = GridBag()
      .fillCellHorizontally()
      .anchor(GridBagConstraints.WEST)

    val topInset = 5
    val bottomInset = if (newLayout) (if (highlightActions) 4 else 10) else 5
    buttons.add(createActionLabel(tooltipAction.text, runFixAction, hintHint.textBackground),
                gridBag.next().insets(topInset, if (newLayout) 10 else 8, bottomInset, 4))
    buttons.add(createKeymapHint(shortcutRunActionText),
                gridBag.next().insets(if (newLayout) topInset else 0, 4, if (newLayout) bottomInset else 0, 12))

    val showAllFixes = { _: InputEvent? ->
      hint.hide()
      tooltipAction.showAllActions(editor)
    }

    buttons.add(createActionLabel("More actions...", showAllFixes, hintHint.textBackground),
                gridBag.next().insets(topInset, 12, bottomInset, 4))
    buttons.add(createKeymapHint(shortcutShowAllActionsText),
                gridBag.next().fillCellHorizontally().insets(if (newLayout) topInset else 0, 4, if (newLayout) bottomInset else 0, 20))

    actions.add(object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) {
        runFixAction(e.inputEvent)
      }

      init {
        registerCustomShortcutSet(runActionCustomShortcutSet, editor.contentComponent)
      }
    })

    actions.add(object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) {
        showAllFixes(e.inputEvent)
      }

      init {
        registerCustomShortcutSet(getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS), editor.contentComponent)
      }
    })

    val buttonsConstraints = GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                                JBUI.insetsTop(0), 0, 0)

    grid.add(wrapper, buttonsConstraints)
  }

  private fun createActionPanelWithBackground(highlight : Boolean): JPanel {
    val wrapper: JPanel = if (highlight) object : JPanel(BorderLayout()) {
      override fun paint(g: Graphics?) {
        g!!.color = UIUtil.getToolTipActionBackground()
        if (JBPopupFactory.getInstance().getParentBalloonFor(this) == null) {
          g.fillRect(0, 0, width, height)
        }
        else {
          val graphics2D = g as Graphics2D
          val cfg = GraphicsConfig(g)
          cfg.setAntialiasing(true)

          graphics2D.fill(RoundRectangle2D.Double(1.0, 0.0, bounds.width - 2.5, (bounds.height / 2).toDouble(), 0.0, 0.0))

          val arc = BalloonImpl.ARC.get().toDouble()
          val double = RoundRectangle2D.Double(1.0, 0.0, bounds.width - 2.5, (bounds.height - 1).toDouble(), arc, arc)

          graphics2D.fill(double)

          cfg.restore()
        }
        super.paint(g)
      }
    } else JPanel(BorderLayout())

    wrapper.isOpaque = false
    wrapper.border = JBUI.Borders.empty()
    return wrapper
  }

  private fun getKeymap(key: String): String {
    val keymapManager = KeymapManager.getInstance()
    if (keymapManager != null) {
      val keymap = keymapManager.activeKeymap
      return KeymapUtil.getShortcutsText(keymap.getShortcuts(key))
    }

    return ""
  }

  private fun createKeymapHint(shortcutRunAction: String): JComponent {
    val fixHint = object : JBLabel(shortcutRunAction) {
      override fun getForeground(): Color {
        return getKeymapColor()
      }
    }
    fixHint.border = JBUI.Borders.empty()
    fixHint.font = getActionFont()
    return fixHint
  }

  override fun createRenderer(text: String?, width: Int): LineTooltipRenderer {
    return DaemonTooltipWithActionRenderer(text, tooltipAction, width, equalityObjects)
  }

  override fun canAutoHideOn(event: TooltipEvent): Boolean {
    if (isOwnAction(event.action)) {
      return false
    }
    if (MenuSelectionManager.defaultManager().selectedPath.isNotEmpty()) {
      return false
    }

    val inputEvent = event.inputEvent
    if (inputEvent is MouseEvent) {
      val source = inputEvent.source
      if (source is ActionMenuItem && isOwnAction(source.anAction)) {
        return false
      }
    }

    return super.canAutoHideOn(event)
  }

  private fun isOwnAction(action: AnAction?) = action is ShowDocAction || action is ShowActionsAction || action is SettingsActionGroup

  private class SettingsActionGroup(actions: List<AnAction>) : DefaultActionGroup(actions), HintManagerImpl.ActionToIgnore, DumbAware {
    init {
      isPopup = true
    }
  }

  override fun isContentAction(dressedText: String?): Boolean {
    return super.isContentAction(dressedText) || tooltipAction != null
  }

  private fun createSettingsComponent(hintHint: HintHint,
                                      reloader: TooltipReloader,
                                      hasMore: Boolean,
                                      newLayout: Boolean): JComponent {
    val presentation = Presentation()
    presentation.icon = AllIcons.Actions.More
    presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, true)
    val actions = mutableListOf<AnAction>()
    actions.add(ShowActionsAction(reloader, tooltipAction != null))
    val docAction = ShowDocAction(reloader, hasMore)
    actions.add(docAction)
    val actionGroup = SettingsActionGroup(actions)
    val buttonSize = if (newLayout) 20 else 18
    val settingsButton = ActionButton(actionGroup, presentation, ActionPlaces.UNKNOWN, Dimension(buttonSize, buttonSize))
    settingsButton.setNoIconsInPopup(true)
    settingsButton.border = JBUI.Borders.empty()
    settingsButton.isOpaque = false

    val wrapper = JPanel(BorderLayout())
    wrapper.add(settingsButton, BorderLayout.EAST)
    wrapper.border = JBUI.Borders.empty()
    wrapper.background = hintHint.textBackground
    wrapper.isOpaque = false
    return wrapper
  }

  private inner class ShowActionsAction(val reloader: TooltipReloader, val isEnabled: Boolean) : ToggleAction(
    "Show Quick Fixes"), HintManagerImpl.ActionToIgnore {

    override fun isSelected(e: AnActionEvent): Boolean {
      return isShowActions()
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      setShowActions(state)
      reloader.reload(myCurrentWidth > 0)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = isEnabled
      super.update(e)
    }
  }

  private inner class ShowDocAction(val reloader: TooltipReloader, val isEnabled: Boolean) : ToggleAction(
    "Show Inspection Description"), HintManagerImpl.ActionToIgnore, DumbAware, PopupAction {

    init {
      shortcutSet = getActiveKeymapShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)
    }

    override fun isSelected(e: AnActionEvent): Boolean {
      return myCurrentWidth > 0
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      TooltipActionsLogger.logShowDescription(e.project, "gear", e.inputEvent, e.place)
      reloader.reload(state)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = isEnabled
      super.update(e)
    }

  }

}


fun createActionLabel(text: String, action: (InputEvent?) -> Unit, background: Color): HyperlinkLabel {
  val label = object : HyperlinkLabel(text, background) {
    override fun getTextOffset(): Int {
      return 0
    }
  }
  label.border = JBUI.Borders.empty()
  label.addHyperlinkListener(object : HyperlinkAdapter() {
    override fun hyperlinkActivated(e: HyperlinkEvent) {
      action(e.inputEvent)
    }
  })
  val toolTipFont = getActionFont()

  label.font = toolTipFont

  return label
}

private fun getKeymapColor(): Color {
  return JBColor.namedColor("ToolTip.Actions.infoForeground", JBColor(0x99a4ad, 0x919191))
}

private fun getActionFont(): Font? {
  val toolTipFont = UIUtil.getToolTipFont()
  if (toolTipFont == null || SystemInfo.isWindows) return toolTipFont

  //if font was changed from default we dont have a good heuristic to customize it
  if (JBFont.label() != toolTipFont || UISettings.instance.overrideLafFonts) return toolTipFont

  if (SystemInfo.isMac) {
    return toolTipFont.deriveFont(toolTipFont.size - 1f)
  }
  if (SystemInfo.isLinux) {
    return toolTipFont.deriveFont(toolTipFont.size - 1f)
  }

  return toolTipFont
}


