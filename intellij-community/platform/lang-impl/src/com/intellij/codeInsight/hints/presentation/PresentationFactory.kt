// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.ui.LightweightHint
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Contract
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.font.FontRenderContext
import java.util.*
import javax.swing.Icon
import javax.swing.UIManager

class PresentationFactory(private val editor: EditorImpl) {
  /**
   * Smaller text, than editor, required to be wrapped with [roundWithBackground]
   */
  @Contract(pure = true)
  fun smallText(text: String): InlayPresentation {
    val fontData = getFontData(editor)
    val plainFont = fontData.font
    val width = editor.contentComponent.getFontMetrics(plainFont).stringWidth(text)
    val ascent = editor.ascent
    val descent = editor.descent
    val height = editor.lineHeight
    val textWithoutBox = InsetPresentation(EffectInlayPresentation(
      TextInlayPresentation(width, fontData.lineHeight, text, fontData.baseline) {
        plainFont
      },
      plainFont, height, ascent, descent
    ), top = 1, down = 1)
    return withInlayAttributes(textWithoutBox)
  }

  /**
   * Text, that is not expected to be drawn with rounding, the same font size as in editor.
   */
  @Contract(pure = true)
  fun text(text: String): InlayPresentation {
    val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
    val width = editor.contentComponent.getFontMetrics(font).stringWidth(text)
    return withInlayAttributes(TextInlayPresentation(
      width,
      editor.lineHeight,
      text,
      editor.ascent
    ) { font })
  }

  /**
   * Adds inlay background and rounding with insets.
   * Intended to be used with [smallText]
   */
  @Contract(pure = true)
  fun roundWithBackground(base: InlayPresentation): InlayPresentation {
    val rounding = rounding(8, 8, withInlayAttributes(BackgroundPresentation(
      InsetPresentation(
        base,
        left = 7,
        right = 7,
        top = 0,
        down = 0
      )
    )))
    val offsetFromTop = getFontData(editor).offsetFromTop(editor)
    return InsetPresentation(rounding, top = offsetFromTop)
  }

  @Contract(pure = true)
  fun icon(icon: Icon): IconPresentation = IconPresentation(icon, editor.component)

  @Contract(pure = true)
  fun withTooltip(tooltip: String, base: InlayPresentation): InlayPresentation = when {
    tooltip.isEmpty() -> base
    else -> onHover(base, tooltipHandler(tooltip))
  }

  @Contract(pure = true)
  fun folding(placeholder: InlayPresentation, unwrapAction: () -> InlayPresentation): InlayPresentation {
    return ChangeOnClickPresentation(changeOnHover(placeholder, onHover = {
      attributes(placeholder) {
        it.with(attributesOf(EditorColors.FOLDED_TEXT_ATTRIBUTES))
      }
    }), onClick = unwrapAction)
  }

  /**
   * Creates node, that can be collapsed/expanded by clicking on prefix/suffix.
   * If presentation is collapsed, clicking to content will expand it.
   */
  @Contract(pure = true)
  fun collapsible(
    prefix: InlayPresentation,
    collapsed: InlayPresentation,
    expanded: () -> InlayPresentation,
    suffix: InlayPresentation,
    startWithPlaceholder: Boolean = true
  ) : InlayPresentation {
    val (matchingPrefix, matchingSuffix) = matchingBraces(prefix, suffix)
    val prefixExposed = EventExposingPresentation(matchingPrefix)
    val suffixExposed = EventExposingPresentation(matchingSuffix)
    var presentationToChange: BiStatePresentation? = null
    val content = BiStatePresentation(first = {
      onClick(collapsed, MouseButton.Left,
              onClick = { m, p ->
        presentationToChange?.flipState()
      })
    }, second = { expanded() }, initialState = startWithPlaceholder)
    presentationToChange = content
    val listener = object: InputHandler {
      override fun mouseClicked(event: MouseEvent, translated: Point) {
        content.flipState()
      }
    }
    prefixExposed.addInputListener(listener)
    suffixExposed.addInputListener(listener)
    return seq(prefixExposed, content, suffixExposed)
  }

  @Contract(pure = true)
  fun matchingBraces(left: InlayPresentation, right: InlayPresentation) : Pair<InlayPresentation, InlayPresentation> {
    val (leftMatching, rightMatching) = matching(listOf(left, right))
    return leftMatching to rightMatching
  }

  @Contract(pure = true)
  fun matching(presentations: List<InlayPresentation>) : List<InlayPresentation> {
    return synchronousOnHover(presentations) { presentation ->
      attributes(presentation) { base ->
        base.with(attributesOf(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES))
      }
    }
  }

  /**
   * On hover of any of [presentations] changes all the presentations with a given decorator.
   * This presentation is stateless.
   */
  @Contract(pure = true)
  fun synchronousOnHover(presentations: List<InlayPresentation>, decorator: (InlayPresentation) -> InlayPresentation) : List<InlayPresentation> {
    val forwardings = presentations.map { DynamicDelegatePresentation(it) }
    return forwardings.map {
      onHover(it) { event ->
        if (event != null) {
          for ((index, forwarding) in forwardings.withIndex()) {
            forwarding.delegate = decorator(presentations[index])
          }
        } else {
          for ((index, forwarding) in forwardings.withIndex()) {
            forwarding.delegate = presentations[index]
          }
        }
      }
    }
  }

  /**
   * @see OnHoverPresentation
   */
  @Contract(pure = true)
  fun onHover(base: InlayPresentation, onHover: (MouseEvent?) -> Unit): InlayPresentation {
    return OnHoverPresentation(base, onHover)
  }

  /**
   * @see OnClickPresentation
   */
  @Contract(pure = true)
  fun onClick(base: InlayPresentation, button: MouseButton, onClick: (MouseEvent, Point) -> Unit): InlayPresentation {
    return OnClickPresentation(base) { e, p ->
      if (button == e.mouseButton) {
        onClick(e, p)
      }
    }
  }

  /**
   * @see OnClickPresentation
   */
  @Contract(pure = true)
  fun onClick(base: InlayPresentation, buttons: EnumSet<MouseButton>, onClick: (MouseEvent, Point) -> Unit): InlayPresentation {
    return OnClickPresentation(base) { e, p ->
      if (e.mouseButton in buttons) {
        onClick(e, p)
      }
    }
  }

  /**
   * @see ChangeOnHoverPresentation
   */
  @Contract(pure = true)
  fun changeOnHover(
    default: InlayPresentation,
    onHover: () -> InlayPresentation,
    onHoverPredicate: (MouseEvent) -> Boolean = { true }
  ): InlayPresentation = ChangeOnHoverPresentation(default, onHover, onHoverPredicate)

  @Contract(pure = true)
  fun reference(base: InlayPresentation, onClickAction: () -> Unit): InlayPresentation {
    val noHighlightReference = onClick(base, MouseButton.Middle) { _, _ ->
      onClickAction()
    }
    return changeOnHover(noHighlightReference, {
      val withRefAttributes = attributes(noHighlightReference) {
        val attributes = attributesOf(EditorColors.REFERENCE_HYPERLINK_COLOR)
        attributes.effectType = null // With underlined looks weird
        it.with(attributes)
      }
      onClick(withRefAttributes, EnumSet.of(MouseButton.Left, MouseButton.Middle)) { _, _ ->
        onClickAction()
      }
    }) { isControlDown(it) }
  }

  @Contract(pure = true)
  fun psiSingleReference(base: InlayPresentation, resolve: () -> PsiElement?): InlayPresentation {
    return reference(base) { navigateInternal(resolve) }
  }

  @Contract(pure = true)
  fun seq(vararg presentations: InlayPresentation): InlayPresentation {
    return when (presentations.size) {
      0 -> SpacePresentation(0, 0)
      1 -> presentations.first()
      else -> SequencePresentation(presentations.toList())
    }
  }

  @Contract(pure = true)
  fun rounding(arcWidth: Int, arcHeight: Int, presentation: InlayPresentation): InlayPresentation =
    RoundPresentation(presentation, arcWidth, arcHeight)

  private fun attributes(base: InlayPresentation, transformer: (TextAttributes) -> TextAttributes): AttributesTransformerPresentation {
    return AttributesTransformerPresentation(base, transformer)
  }

  private fun withInlayAttributes(base: InlayPresentation): InlayPresentation {
    return AttributesTransformerPresentation(base) {
      it.withDefault(attributesOf(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT))
    }
  }

  private fun isControlDown(e: MouseEvent): Boolean = (SystemInfo.isMac && e.isMetaDown) || e.isControlDown

  private fun showTooltip(editor: Editor, e: MouseEvent, text: String): LightweightHint {
    val hint = run {
      val label = HintUtil.createInformationLabel(text)
      label.border = JBUI.Borders.empty(6, 6, 5, 6)
      LightweightHint(label)
    }

    val constraint = HintManager.ABOVE

    val point = run {
      val pointOnEditor = locationAt(e, editor.contentComponent)
      val p = HintManagerImpl.getHintPosition(hint, editor, editor.xyToVisualPosition(pointOnEditor), constraint)
      p.x = e.xOnScreen - editor.contentComponent.topLevelAncestor.locationOnScreen.x
      p
    }

    HintManagerImpl.getInstanceImpl().showEditorHint(hint, editor, point,
                                                     HintManager.HIDE_BY_ANY_KEY
                                                       or HintManager.HIDE_BY_TEXT_CHANGE
                                                       or HintManager.HIDE_BY_SCROLLING,
                                                     0,
                                                     false,
                                                     HintManagerImpl.createHintHint(editor, point, hint, constraint).setContentActive(false)
    )

    return hint
  }

  private fun tooltipHandler(tooltip: String): (MouseEvent?) -> Unit {
    var hint: LightweightHint? = null
    return { event ->
      when (event) {
        null -> {
          hint?.hide()
          hint = null
        }
        else -> {
          if (hint?.isVisible != true) {
            hint = showTooltip(editor, event, tooltip)
          }
        }
      }
    }
  }

  private fun locationAt(e: MouseEvent, component: Component): Point {
    val pointOnScreen = component.locationOnScreen
    return Point(e.xOnScreen - pointOnScreen.x, e.yOnScreen - pointOnScreen.y)
  }

  private fun attributesOf(key: TextAttributesKey?) = editor.colorsScheme.getAttributes(key) ?: TextAttributes()

  private fun navigateInternal(resolve: () -> PsiElement?) {
    val target = resolve()
    if (target != null) {
      if (target is Navigatable) {
        CommandProcessor.getInstance().executeCommand(target.project, { target.navigate(true) }, null, null)
      }
    }
  }

  private class FontData constructor(editor: Editor, familyName: String, size: Int) {
    val metrics: FontMetrics
    val lineHeight: Int
    val baseline: Int

    val font: Font
      get() = metrics.font

    init {
      val font = UIUtil.getFontWithFallback(familyName, Font.PLAIN, size)
      val context = getCurrentContext(editor)
      metrics = FontInfo.getFontMetrics(font, context)
      // We assume this will be a better approximation to a real line height for a given font
      lineHeight = Math.ceil(font.createGlyphVector(context, "Albpq@").visualBounds.height).toInt()
      baseline = Math.ceil(font.createGlyphVector(context, "Alb").visualBounds.height).toInt()
    }

    fun isActual(editor: Editor, familyName: String, size: Int): Boolean {
      val font = metrics.font
      if (familyName != font.family || size != font.size) return false
      val currentContext = getCurrentContext(editor)
      return currentContext.equals(metrics.fontRenderContext)
    }

    private fun getCurrentContext(editor: Editor): FontRenderContext {
      val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
      return FontRenderContext(editorContext.transform,
                               AntialiasingType.getKeyForCurrentScope(false),
                               if (editor is EditorImpl)
                                 editor.myFractionalMetricsHintValue
                               else
                                 RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
    }

    /**
     * Offset from the top edge of drawing rectangle to rectangle with text.
     */
    fun offsetFromTop(editor: Editor): Int = (editor.lineHeight - lineHeight) / 2
  }

  private fun getFontData(editor: Editor): FontData {
    val familyName = UIManager.getFont("Label.font").family
    val size = Math.max(1, editor.colorsScheme.editorFontSize - 1)
    var metrics = editor.getUserData(FONT_DATA)
    if (metrics != null && !metrics.isActual(editor, familyName, size)) {
      metrics = null
    }
    if (metrics == null) {
      metrics = FontData(editor, familyName, size)
      editor.putUserData(FONT_DATA, metrics)
    }
    return metrics
  }

  companion object {
    @JvmStatic
    private val FONT_DATA = Key.create<FontData>("InlayHintFontData")
  }
}

private fun TextAttributes.with(other: TextAttributes): TextAttributes {
  val result = this.clone()
  other.foregroundColor?.let { result.foregroundColor = it }
  other.backgroundColor?.let { result.backgroundColor = it }
  other.fontType.let { result.fontType = it }
  other.effectType?.let { result.effectType = it }
  other.effectColor?.let { result.effectColor = it }
  return result
}

private fun TextAttributes.withDefault(other: TextAttributes): TextAttributes {
  val result = this.clone()
  if (result.foregroundColor == null) {
    result.foregroundColor = other.foregroundColor
  }
  if (result.backgroundColor == null) {
    result.backgroundColor = other.backgroundColor
  }
  if (result.effectType == null) {
    result.effectType = other.effectType
  }
  if (result.effectColor == null) {
    result.effectColor = other.effectColor
  }
  return result
}