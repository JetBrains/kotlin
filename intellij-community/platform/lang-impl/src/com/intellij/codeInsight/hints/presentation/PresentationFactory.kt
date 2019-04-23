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
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.ui.LightweightHint
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.font.FontRenderContext
import javax.swing.Icon
import javax.swing.UIManager

class PresentationFactory(val editor: EditorImpl) {
  // TODO document, that this is not the same font (type, size) as in editor!
  fun text(text: String): InlayPresentation {
    val fontData = getFontData(editor)
    val metrics = fontData.metrics
    val plainFont = metrics.font
    val width = editor.contentComponent.getFontMetrics(plainFont).stringWidth(text)
    val ascent = editor.ascent
    val descent = editor.descent
    val height = editor.lineHeight
//    val yBaseline = Math.max(ascent, (height + metrics.ascent - metrics.descent) / 2) - 1
    val textWithoutBox = EffectInlayPresentation(
      TextInlayPresentation(width, fontData.lineHeight, text, fontData.baseline) {
        plainFont
      },
      plainFont, height, ascent, descent
    )
    return withInlayAttributes(textWithoutBox)
  }

  fun roundWithBackground(base: InlayPresentation): InlayPresentation {
    val rounding = rounding(8, 8, withInlayAttributes(BackgroundInlayPresentation(
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

  private fun withInlayAttributes(base: InlayPresentation): InlayPresentation {
    return AttributesTransformerPresentation(base) {
      it.withDefault(attributesOf(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT))
    }
  }

  fun singleText(text: String) : InlayPresentation {
    return roundWithBackground(text(text))
  }

  fun icon(icon: Icon) : IconPresentation = IconPresentation(icon, editor.component)

  fun roundedText(text: String): InlayPresentation {
    return rounding(8, 8, text(text))
  }

  fun hyperlink(base: InlayPresentation): InlayPresentation {
    val dynamic = DynamicPresentation(base)
    // TODO only with ctrl
    return onHover(dynamic) { event ->
      if (event != null) {
        dynamic.delegate = AttributesTransformerPresentation(base) {
          val attributes = attributesOf(EditorColors.REFERENCE_HYPERLINK_COLOR)
          attributes.effectType = null // With underlined looks weird
          it.with(attributes)
        }
      } else {
        dynamic.delegate = base
      }
    }
  }

  fun tooltip(e: MouseEvent, text: String) {
    val label = HintUtil.createInformationLabel(text)
    label.border = JBUI.Borders.empty(6, 6, 5, 6)

  }

  fun withTooltip(tooltip: String, base: InlayPresentation): InlayPresentation = when {
    tooltip.isEmpty() -> base
    else -> onHover(base, tooltipHandler(tooltip))
  }

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

  private fun tooltipHandler(tooltip: String) : (MouseEvent?) -> Unit {
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

  private fun locationAt(e: MouseEvent, component: Component): Point  {
    val pointOnScreen = component.locationOnScreen
    return Point(e.xOnScreen - pointOnScreen.x, e.yOnScreen - pointOnScreen.y)
  }

  fun folding(placeholder: InlayPresentation, unwrapAction: () -> InlayPresentation): InlayPresentation {
    // TODO add folding style
    var dynamic: DynamicPresentation? = null
    val placeholderOnClick = onClick(placeholder) { _, _ ->
      dynamic?.delegate = unwrapAction()
    }
    dynamic = DynamicPresentation(placeholderOnClick)
    return dynamic
  }

  fun asWrongReference(presentation: InlayPresentation) : InlayPresentation {
    return AttributesTransformerPresentation(presentation) {
      it.with(attributesOf(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES))
    }
  }

  private fun attributesOf(key: TextAttributesKey?) = editor.colorsScheme.getAttributes(key) ?: TextAttributes()

  fun onHover(base: InlayPresentation, onHover: (MouseEvent?) -> Unit) : InlayPresentation {
    return OnHoverPresentation(base, onHover)
  }

  fun onClick(base: InlayPresentation, onClick: (MouseEvent, Point) -> Unit) : InlayPresentation {
    return OnClickPresentation(base, onClick)
  }


  // TODO ctrl + cmd handling (+ middle click)
  // TODO resolve should not include PsiElement! Otherwise here may be memory leak?
  fun navigateSingle(base: InlayPresentation, resolve: () -> PsiElement?): InlayPresentation {
    return onClick(hyperlink(base)) { _, _ ->
      val target = resolve()
      if(target != null) {
        if (target is Navigatable) {
            CommandProcessor.getInstance().executeCommand(target.project, { target.navigate(true) }, null, null)
        }
      }
    }
  }

  fun seq(vararg presentations: InlayPresentation) : InlayPresentation {
    return when (presentations.size) {
      0 -> SpacePresentation(0, 0)
      1 -> presentations.first()
      else -> SequencePresentation(presentations.toList())
    }
  }

  fun rounding(arcWidth: Int, arcHeight: Int, presentation: InlayPresentation): InlayPresentation =
    RoundPresentation(presentation, arcWidth, arcHeight)

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
      lineHeight = Math.ceil(font.createGlyphVector(context, "Albp").visualBounds.height).toInt()
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
    fun offsetFromTop(editor: Editor) : Int = (editor.lineHeight - lineHeight) / 2
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

  private fun getCurrentContext(editor: Editor): FontRenderContext {
    val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
    return FontRenderContext(editorContext.transform,
                             AntialiasingType.getKeyForCurrentScope(false),
                             if (editor is EditorImpl)
                               editor.myFractionalMetricsHintValue
                             else
                               RenderingHints.VALUE_FRACTIONALMETRICS_OFF)
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