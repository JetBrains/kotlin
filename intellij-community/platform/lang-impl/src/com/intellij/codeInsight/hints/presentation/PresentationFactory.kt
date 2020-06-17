// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.InlayPresentationFactory.*
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.SystemInfo
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.ui.LightweightHint
import com.intellij.util.ui.JBUI
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Contract
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Icon
import kotlin.math.max

/**
 * Contains non-stable and not well-designed API. Will be changed in 2020.2
 */
@ApiStatus.Experimental
class PresentationFactory(private val editor: EditorImpl) : InlayPresentationFactory {
  private val textMetricsStorage = InlayTextMetricsStorage(editor)

  @Contract(pure = true)
  override fun smallText(text: String): InlayPresentation {
    val textWithoutBox = InsetPresentation(TextInlayPresentation(textMetricsStorage, true, text), top = 1, down = 1)
    return withInlayAttributes(textWithoutBox)
  }

  override fun container(
    presentation: InlayPresentation,
    padding: Padding?,
    roundedCorners: RoundedCorners?,
    background: Color?,
    backgroundAlpha: Float
  ): InlayPresentation {
    return ContainerInlayPresentation(presentation, padding, roundedCorners, background, backgroundAlpha)
  }


  override fun mouseHandling(base: InlayPresentation, clickListener: ClickListener?, hoverListener: HoverListener?): InlayPresentation {
    return MouseHandlingPresentation(base, clickListener, hoverListener)
  }

  @Contract(pure = true)
  @Deprecated(message = "Bad API for Java, use mouseHandling with ClickListener")
  fun mouseHandling(
    base: InlayPresentation,
    clickListener: ((MouseEvent, Point) -> Unit)?,
    hoverListener: HoverListener?
  ): InlayPresentation {
    val adapter = if (clickListener != null) {
      object : ClickListener {
        override fun onClick(event: MouseEvent, translated: Point) {
          clickListener.invoke(event, translated)
        }
      }
    }
    else {
      null
    }
    return mouseHandling(base, adapter, hoverListener)
  }

  @Contract(pure = true)
  override fun text(text: String): InlayPresentation {
    return withInlayAttributes(TextInlayPresentation(textMetricsStorage, false, text))
  }

  /**
   * Adds inlay background and rounding with insets.
   * Intended to be used with [smallText]
   */
  @Contract(pure = true)
  fun roundWithBackground(base: InlayPresentation): InlayPresentation {
    val rounding = withInlayAttributes(RoundWithBackgroundPresentation(
      InsetPresentation(
        base,
        left = 7,
        right = 7,
        top = 0,
        down = 0

      ),
      8,
      8
    ))
    val offsetFromTop = textMetricsStorage.getFontMetrics(true).offsetFromTop()
    return InsetPresentation(rounding, top = offsetFromTop)
  }

  @Contract(pure = true)
  override fun icon(icon: Icon): IconPresentation = IconPresentation(icon, editor.component)

  @Contract(pure = true)
  fun folding(placeholder: InlayPresentation, unwrapAction: () -> InlayPresentation): InlayPresentation {
    return ChangeOnClickPresentation(changeOnHover(placeholder, onHover = {
      attributes(placeholder) {
        it.with(attributesOf(EditorColors.FOLDED_TEXT_ATTRIBUTES))
      }
    }), onClick = unwrapAction)
  }

  @Contract(pure = true)
  fun inset(base: InlayPresentation, left: Int = 0, right: Int = 0, top: Int = 0, down: Int = 0): InsetPresentation {
    return InsetPresentation(base, left, right, top, down)
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
    startWithPlaceholder: Boolean = true): InlayPresentation {
    val (matchingPrefix, matchingSuffix) = matchingBraces(prefix, suffix)
    var presentationToChange: BiStatePresentation? = null

    val content = BiStatePresentation(first = {
      onClick(collapsed, MouseButton.Left,
              onClick = { _, _ ->
                presentationToChange?.flipState()
              })
    }, second = { expanded() }, initiallyFirstEnabled = startWithPlaceholder)
    presentationToChange = content

    class ContentFlippingPresentation(base: InlayPresentation) : StaticDelegatePresentation(base) {
      override fun mouseClicked(event: MouseEvent, translated: Point) {
        content.flipState()
      }
    }

    val prefixExposed = ContentFlippingPresentation(matchingPrefix)
    val suffixExposed = ContentFlippingPresentation(matchingSuffix)
    return seq(prefixExposed, content, suffixExposed)
  }

  @Contract(pure = true)
  fun matchingBraces(left: InlayPresentation, right: InlayPresentation): Pair<InlayPresentation, InlayPresentation> {
    val (leftMatching, rightMatching) = matching(listOf(left, right))
    return leftMatching to rightMatching
  }

  @Contract(pure = true)
  fun matching(presentations: List<InlayPresentation>): List<InlayPresentation> = synchronousOnHover(presentations) { presentation ->
    attributes(presentation) { base ->
      base.with(attributesOf(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES))
    }
  }

  /**
   * On hover of any of [presentations] changes all the presentations with a given decorator.
   * This presentation is stateless.
   */
  @Contract(pure = true)
  fun synchronousOnHover(presentations: List<InlayPresentation>,
                         decorator: (InlayPresentation) -> InlayPresentation): List<InlayPresentation> {
    val forwardings = presentations.map { DynamicDelegatePresentation(it) }
    return forwardings.map {
      onHover(it, object : HoverListener {
        override fun onHover(event: MouseEvent, translated: Point) {
          for ((index, forwarding) in forwardings.withIndex()) {
            forwarding.delegate = decorator(presentations[index])
          }
        }

        override fun onHoverFinished() {
          for ((index, forwarding) in forwardings.withIndex()) {
            forwarding.delegate = presentations[index]
          }
        }
      })
    }
  }

  /**
   * @see OnHoverPresentation
   */
  @Contract(pure = true)
  fun onHover(base: InlayPresentation, onHoverListener: HoverListener): InlayPresentation =
    OnHoverPresentation(base, onHoverListener)

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
    base: InlayPresentation,
    onHover: () -> InlayPresentation,
    onHoverPredicate: (MouseEvent) -> Boolean = { true }
  ): InlayPresentation = ChangeOnHoverPresentation(base, onHover, onHoverPredicate)

  @Contract(pure = true)
  fun reference(base: InlayPresentation, onClickAction: () -> Unit): InlayPresentation {
    return reference(
      base = base,
      onClickAction = Runnable { onClickAction() },
      clickButtonsWithoutHover = EnumSet.of(MouseButton.Middle),
      clickButtonsWithHover = EnumSet.of(MouseButton.Left, MouseButton.Middle),
      hoverPredicate = { isControlDown(it) }
    )
  }

  @Contract(pure = true)
  fun referenceOnHover(base: InlayPresentation, clickListener: ClickListener): InlayPresentation {
    val delegate = DynamicDelegatePresentation(base)
    val hovered = onClick(
      base = withReferenceAttributes(base),
      buttons = EnumSet.of(MouseButton.Left, MouseButton.Middle),
      onClick = { e, p ->
        clickListener.onClick(e, p)
      }
    )
    return OnHoverPresentation(delegate, object : HoverListener {
      override fun onHover(event: MouseEvent, translated: Point) {
        val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        editor.setCustomCursor(this@PresentationFactory, handCursor)
        delegate.delegate = hovered
      }

      override fun onHoverFinished() {
        delegate.delegate = base
        editor.setCustomCursor(this@PresentationFactory, null)
      }
    })
  }

  @Contract(pure = true)
  private fun reference(
    base: InlayPresentation,
    onClickAction: Runnable,
    clickButtonsWithoutHover: EnumSet<MouseButton>,
    clickButtonsWithHover: EnumSet<MouseButton>,
    hoverPredicate: (MouseEvent) -> Boolean
  ): InlayPresentation {
    val noHighlightReference = onClick(base, clickButtonsWithoutHover) { _, _ ->
      onClickAction.run()
    }
    return changeOnHover(noHighlightReference, {
      return@changeOnHover onClick(withReferenceAttributes(noHighlightReference), clickButtonsWithHover) { _, _ ->
        onClickAction.run()
      }
    }, hoverPredicate)
  }

  private fun withReferenceAttributes(noHighlightReference: InlayPresentation): AttributesTransformerPresentation {
    return attributes(noHighlightReference) {
      val attributes = attributesOf(EditorColors.REFERENCE_HYPERLINK_COLOR)
      attributes.effectType = null // With underlined looks weird
      it.with(attributes)
    }
  }

  @Contract(pure = true)
  fun psiSingleReference(base: InlayPresentation, resolve: () -> PsiElement?): InlayPresentation =
    reference(base) { navigateInternal(resolve) }

  @Contract(pure = true)
  fun seq(vararg presentations: InlayPresentation): InlayPresentation {
    return when (presentations.size) {
      0 -> SpacePresentation(0, 0)
      1 -> presentations.first()
      else -> SequencePresentation(presentations.toList())
    }
  }

  fun join(presentations: List<InlayPresentation>, separator: () -> InlayPresentation): InlayPresentation {
    val seq = mutableListOf<InlayPresentation>()
    var first = true
    for (presentation in presentations) {
      if (!first) {
        seq.add(separator())
      }
      seq.add(presentation)
      first = false
    }
    return SequencePresentation(seq)
  }

  fun button(default: InlayPresentation, clicked: InlayPresentation, clickListener: ClickListener?, hoverListener: HoverListener?) : InlayPresentation {
    val defaultOrClicked: BiStatePresentation = object : BiStatePresentation({ default }, { clicked }, false) {
      override val width: Int
        get() = max(default.width, clicked.width)

      override val height: Int
        get() = max(default.height, clicked.height)
    }
    return object : StaticDelegatePresentation(defaultOrClicked) {
      override fun mouseClicked(event: MouseEvent, translated: Point) {
        clickListener?.onClick(event, translated)
        defaultOrClicked.flipState()
      }

      override fun mouseMoved(event: MouseEvent, translated: Point) {
        hoverListener?.onHover(event, translated)
      }

      override fun mouseExited() {
        hoverListener?.onHoverFinished()
      }
    }
  }

  private fun attributes(base: InlayPresentation, transformer: (TextAttributes) -> TextAttributes): AttributesTransformerPresentation =
    AttributesTransformerPresentation(base, transformer)

  private fun withInlayAttributes(base: InlayPresentation): InlayPresentation {
    return AttributesTransformerPresentation(base) {
      it.withDefault(attributesOf(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT))
    }
  }

  private fun isControlDown(e: MouseEvent): Boolean = (SystemInfo.isMac && e.isMetaDown) || e.isControlDown

  @Contract(pure = true)
  fun withTooltip(tooltip: String, base: InlayPresentation): InlayPresentation = when {
    tooltip.isEmpty() -> base
    else -> {
      var hint: LightweightHint? = null
      onHover(base, object : HoverListener {
        override fun onHover(event: MouseEvent, translated: Point) {
          if (hint?.isVisible != true) {
            hint = showTooltip(editor, event, tooltip)
          }
        }

        override fun onHoverFinished() {
          hint?.hide()
          hint = null
        }
      })
    }
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

  private fun locationAt(e: MouseEvent, component: Component): Point {
    val pointOnScreen = component.locationOnScreen
    return Point(e.xOnScreen - pointOnScreen.x, e.yOnScreen - pointOnScreen.y)
  }

  private fun attributesOf(key: TextAttributesKey) = editor.colorsScheme.getAttributes(key) ?: TextAttributes()

  private fun navigateInternal(resolve: () -> PsiElement?) {
    val target = resolve()
    if (target is Navigatable) {
      CommandProcessor.getInstance().executeCommand(target.project, { target.navigate(true) }, null, null)
    }
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