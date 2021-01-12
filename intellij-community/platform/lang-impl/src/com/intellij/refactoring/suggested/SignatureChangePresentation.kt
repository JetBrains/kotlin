// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.ide.ui.AntialiasingType
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.diff.DiffColors
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.refactoring.suggested.SignatureChangePresentationModel.Effect
import com.intellij.refactoring.suggested.SignatureChangePresentationModel.TextFragment
import com.intellij.ui.JBColor
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import kotlin.math.*

class SignatureChangePresentation(
  private val model: SignatureChangePresentationModel,
  private val font: Font,
  colorsScheme: EditorColorsScheme,
  private val verticalMode: Boolean
) {
  private val defaultForegroundColor = JBColor.namedColor("Label.foreground", Color.black)
  private val modifiedAttributes = colorsScheme.getAttributes(DiffColors.DIFF_MODIFIED)
  private val addedAttributes = colorsScheme.getAttributes(DiffColors.DIFF_INSERTED)

  private val crossStroke = BasicStroke(1f)
  private val connectionStroke = BasicStroke(connectionLineThickness.toFloat())
  private val connectionColor = modifiedAttributes.backgroundColor ?: defaultForegroundColor

  private val dummyFontRenderContext = FontRenderContext(
    AffineTransform(),
    AntialiasingType.getKeyForCurrentScope(false),
    UISettings.PREFERRED_FRACTIONAL_METRICS_VALUE
  )

  val requiredSize by lazy {
    val oldSignatureSize = signatureDimensions(model.oldSignature, dummyFontRenderContext)
    val newSignatureSize = signatureDimensions(model.newSignature, dummyFontRenderContext)
    if (verticalMode) {
      Dimension(
        oldSignatureSize.width + newSignatureSize.width + betweenSignaturesHSpace + leftSpace + rightSpace,
        max(oldSignatureSize.height, newSignatureSize.height) + topSpace + bottomSpace
      )
    }
    else {
      val size = Dimension(
        max(oldSignatureSize.width, newSignatureSize.width) + leftSpace + rightSpace,
        oldSignatureSize.height + newSignatureSize.height + betweenSignaturesVSpace + topSpace + bottomSpace
      )
      if (model.oldSignature.any { it.connectionId != null }) {
        val router = renderAll(null, dummyFontRenderContext, Rectangle(Point(), size)) as HorizontalModeConnectionRouter
        if (router.hSegmentLevelsRequired > 0) {
          size.height += betweenSignaturesVSpaceWithOneHSegment - betweenSignaturesVSpace +
                         betweenHSegmentsVSpace * (router.hSegmentLevelsRequired - 1)
        }
      }
      size
    }
  }

  private fun signatureDimensions(fragments: List<TextFragment>, context: FontRenderContext): Dimension {
    if (verticalMode) {
      var maxWidth = 0
      var currentLineWidth = 0
      var lines = 1
      fragments.forAllFragments { fragment ->
        when (fragment) {
          is TextFragment.Group -> { } // children processed by forAllFragments()

          is TextFragment.Leaf -> {
            currentLineWidth += font.getStringBounds(fragment.text, context).width.ceilToInt()
          }

          is TextFragment.LineBreak -> {
            maxWidth = max(maxWidth, currentLineWidth)
            currentLineWidth = 0
            if (fragment.indentAfter) {
              currentLineWidth += (font.getStringBounds(" ", context).width * indentInVerticalMode).roundToInt()
            }
            lines++
          }
        }
      }

      return Dimension(maxWidth, lines * lineHeight(context))
    }
    else {
      var width = 0
      fragments.forAllFragments { fragment ->
        when (fragment) {
          is TextFragment.Group -> { } // children processed by forAllFragments()
          is TextFragment.Leaf -> width += font.getStringBounds(fragment.text, context).width.ceilToInt()
          is TextFragment.LineBreak -> width += font.getStringBounds(fragment.spaceInHorizontalMode, context).width.ceilToInt()
        }
      }
      return Dimension(width, lineHeight(context))
    }
  }

  fun paint(g: Graphics2D, bounds: Rectangle) {
    renderAll(g, g.fontRenderContext, bounds)
  }

  private fun renderAll(g: Graphics2D?, fontRenderContext: FontRenderContext, bounds: Rectangle): ConnectionRouter {
    if (g != null) {
      UISettings.setupAntialiasing(g)
    }

    val lineHeight = lineHeight(fontRenderContext)
    val oldSignatureSize = signatureDimensions(model.oldSignature, fontRenderContext)
    val newSignatureSize = signatureDimensions(model.newSignature, fontRenderContext)

    val oldSignatureTop: Int
    val newSignatureTop: Int
    val oldSignatureLeft: Int
    val newSignatureLeft: Int
    if (verticalMode) {
      oldSignatureTop = bounds.y + topSpace
      newSignatureTop = oldSignatureTop

      oldSignatureLeft = bounds.left + leftSpace
      newSignatureLeft = bounds.right - newSignatureSize.width
    }
    else {
      oldSignatureTop = bounds.y + topSpace
      newSignatureTop = bounds.y + bounds.height - lineHeight - bottomSpace

      val width = max(oldSignatureSize.width, newSignatureSize.width)
      val left = bounds.x + (bounds.width - width) / 2 + leftSpace
      oldSignatureLeft = left
      newSignatureLeft = left
    }

    val oldFragmentBounds = SignatureRenderer(g, fontRenderContext, oldSignatureLeft, oldSignatureTop).render(model.oldSignature)
    val newFragmentBounds = SignatureRenderer(g, fontRenderContext, newSignatureLeft, newSignatureTop).render(model.newSignature)

    val newFragmentBoundsById = mutableMapOf<Any, Rectangle>()
    for ((fragment, fragmentBounds) in newFragmentBounds.entries) {
      fragment.connectionId?.let {
        newFragmentBoundsById[it] = fragmentBounds
      }
    }

    val connectedBounds = mutableListOf<Pair<Rectangle, Rectangle>>()
    for ((oldFragment, oldBounds) in oldFragmentBounds.entries) {
      oldFragment.connectionId?.let {
        val newBounds = newFragmentBoundsById[it]!!
        connectedBounds.add(oldBounds to newBounds)
      }
    }

    g?.stroke = connectionStroke
    g?.color = connectionColor
    val connectionRouter = if (verticalMode)
      VerticalModeConnectionRouter(g, newSignatureLeft, oldSignatureLeft + oldSignatureSize.width)
    else
      HorizontalModeConnectionRouter(g, oldSignatureTop + lineHeight, newSignatureTop)
    connectionRouter.drawConnections(connectedBounds)

    return connectionRouter
  }

  private inner class SignatureRenderer(
    private val g: Graphics2D?,
    private val fontRenderContext: FontRenderContext,
    private val left: Int,
    private val top: Int
  ) {
    private val lineHeight = lineHeight(fontRenderContext)

    private var currentX = left
    private var currentY = top
    private val fragmentBounds = mutableMapOf<TextFragment, Rectangle>()

    fun render(fragments: List<TextFragment>): Map<TextFragment, Rectangle> {
      currentX = left
      currentY = top
      drawFragments(fragments, Effect.None, null)
      return fragmentBounds
    }

    private fun drawFragments(fragments: List<TextFragment>, inheritedEffect: Effect, inheritedFontStyle: Int?) {
      for (fragment in fragments) {
        val fragmentStartX = currentX
        val fragmentStartY = currentY

        val effect = inheritedEffect.takeIf { it != Effect.None } ?: fragment.effect

        val fontStyle = inheritedFontStyle ?: when (fragment.effect) {
          Effect.Modified -> Font.BOLD
          Effect.Added, Effect.Removed -> if (inheritedEffect != Effect.None) Font.BOLD else null
          else -> null
        }

        when (fragment) {
          is TextFragment.Leaf -> {
            val font = if (fontStyle != null) font.deriveFont(fontStyle) else font
            currentX = drawText(fragment.text, currentX, currentY, effect, font)
          }

          is TextFragment.LineBreak -> {
            if (verticalMode) {
              currentY += lineHeight
              currentX = left
              if (fragment.indentAfter) {
                currentX += (font.getStringBounds(" ", fontRenderContext).width * indentInVerticalMode).roundToInt()
              }
            }
            else {
              currentX = drawText(fragment.spaceInHorizontalMode, currentX, currentY, effect, font)
            }
          }

          is TextFragment.Group -> {
            drawFragments(fragment.children, effect, fontStyle)
          }
        }

        fragmentBounds[fragment] = Rectangle(
          fragmentStartX - backgroundGap,
          fragmentStartY - backgroundGap,
          currentX - fragmentStartX + backgroundGap * 2,
          currentY - fragmentStartY + lineHeight + backgroundGap * 2
        )
      }
    }

    private fun drawText(
      text: String,
      x: Int,
      y: Int,
      effect: Effect,
      font: Font
    ): Int {
      val backgroundColor = backgroundColor(effect)
      val foregroundColor = foregroundColor(effect) ?: defaultForegroundColor

      val metrics = font.getLineMetrics(text, fontRenderContext)

      val newX = x + font.getStringBounds(text, fontRenderContext).width.ceilToInt()

      if (g != null) {
        if (backgroundColor != null) {
          g.color = backgroundColor
          g.fillRect(x - backgroundGap, y - backgroundGap, newX - x + backgroundGap * 2, lineHeight + backgroundGap * 2)
        }

        g.color = foregroundColor
        g.font = font
        g.drawString(text, x.toFloat(), y.toFloat() + metrics.ascent)

        if (effect == Effect.Removed) {
          g.stroke = crossStroke
          g.color = foregroundColor
          val lineY = y + lineHeight / 2
          g.drawLine(x, lineY, newX, lineY)
        }
      }

      return newX
    }

    private fun backgroundColor(effect: Effect): Color? {
      return when (effect) {
        Effect.Modified, Effect.Moved -> modifiedAttributes.backgroundColor
        Effect.Added -> addedAttributes.backgroundColor
        else -> null
      }
    }

    private fun foregroundColor(effect: Effect): Color? {
      return when (effect) {
        Effect.Modified, Effect.Moved -> modifiedAttributes.foregroundColor
        Effect.Added -> addedAttributes.foregroundColor
        else -> null
      }
    }
  }

  private interface ConnectionRouter {
    fun drawConnections(connectedBounds: List<Pair<Rectangle, Rectangle>>)
  }

  private class HorizontalModeConnectionRouter(
    private val g: Graphics2D?,
    private val oldSignatureBottom: Int,
    private val newSignatureTop: Int
  ) : ConnectionRouter {
    private val connectionsByHSegmentLevel = mutableListOf<MutableList<ConnectionData>>()

    val hSegmentLevelsRequired: Int
      get() = connectionsByHSegmentLevel.size

    override fun drawConnections(connectedBounds: List<Pair<Rectangle, Rectangle>>) {
      val connections = prepareConnectionData(connectedBounds)

      val connectors = mutableListOf<VerticalConnectorData>()

      for (connection in connections) {
        if (connection.oldX == connection.newX) {
          connectors.add(VerticalConnectorData(connection.oldX, oldSignatureBottom, newSignatureTop))
          drawVerticalConnection(connection.oldX)
        }
        else {
          val level = connectionsByHSegmentLevel.firstOrNull { connectionsInLevel ->
            connectionsInLevel.none { it.overlapsHorizontally(connection) }
          }
          if (level != null) {
            level += connection
          }
          else {
            connectionsByHSegmentLevel += mutableListOf(connection)
          }
        }
      }
      if (connectionsByHSegmentLevel.isEmpty()) return

      val firstLevelY = oldSignatureBottom +
                        (newSignatureTop - oldSignatureBottom - (connectionsByHSegmentLevel.size - 1) * betweenHSegmentsVSpace) / 2

      fun levelY(level: Int) = firstLevelY + level * betweenHSegmentsVSpace

      for ((level, connectionsInLevel) in connectionsByHSegmentLevel.withIndex()) {
        for ((oldX, newX) in connectionsInLevel) {
          val levelY = levelY(level)
          connectors.add(VerticalConnectorData(oldX, oldSignatureBottom, levelY))
          connectors.add(VerticalConnectorData(newX, levelY, newSignatureTop))
        }
      }

      for ((level, connectionsInLevel) in connectionsByHSegmentLevel.withIndex()) {
        for ((oldX, newX) in connectionsInLevel) {
          drawConnection(oldX, newX, levelY(level), connectors)
        }
      }
    }

    private fun prepareConnectionData(connectedBounds: List<Pair<Rectangle, Rectangle>>): List<ConnectionData> {
      val occupiedConnectorX = mutableListOf<Int>()

      fun chooseConnectorX(x: Int, step: Int): Int {
        if (occupiedConnectorX.none { abs(it - x) < betweenConnectorsHSpace }) {
          occupiedConnectorX.add(x)
          return x
        }
        return chooseConnectorX(x + step, step)
      }

      val data = mutableListOf<ConnectionData>()
      for ((oldBounds, newBounds) in connectedBounds) {
        var oldX: Int
        var newX: Int
        val oldXStep: Int
        val newXStep: Int
        when {
          oldBounds.right <= newBounds.left -> {
            oldX = oldBounds.right - connectionLineThickness / 2
            newX = newBounds.left + connectionLineThickness / 2
            oldXStep = -betweenConnectorsHSpace
            newXStep = +betweenConnectorsHSpace
          }

          oldBounds.left >= newBounds.right -> {
            oldX = oldBounds.left + connectionLineThickness / 2
            newX = newBounds.right - connectionLineThickness / 2
            oldXStep = +betweenConnectorsHSpace
            newXStep = -betweenConnectorsHSpace
          }

          else -> {
            val left = max(oldBounds.left, newBounds.left)
            val right = min(oldBounds.right, newBounds.right)
            oldX = (left + right) / 2
            newX = oldX
            oldXStep = 0
            newXStep = 0
          }
        }

        if (oldX != newX) {
          if (abs(oldX - newX) < minHSegmentLength) {
            oldX = oldBounds.centerX.roundToInt()
            newX = newBounds.centerX.roundToInt()
          }
          oldX = chooseConnectorX(oldX, oldXStep)
          newX = chooseConnectorX(newX, newXStep)
        }
        data += ConnectionData(oldX, newX)
      }
      return data
    }

    private fun drawConnection(oldX: Int, newX: Int, levelY: Int, connectors: List<VerticalConnectorData>) {
      val oldXD = oldX.toDouble()
      val newXD = newX.toDouble()
      val levelYD = levelY.toDouble()

      val d = rectangularConnectionArcR * 2
      g?.draw(GeneralPath().apply {
        moveTo(oldXD, oldSignatureBottom.toDouble())
        lineTo(oldXD, levelYD - d / 2)
        if (oldXD < newXD) {
          append(Arc2D.Double(oldXD, levelYD - d, d, d, 180.0, 90.0, Arc2D.OPEN), false)
          moveTo(oldXD + d / 2, levelYD)
          horizontalSegmentWithInterruptions(levelYD, oldXD + d / 2, newXD - d / 2, connectors)
          append(Arc2D.Double(newXD - d, levelYD, d, d, 0.0, 90.0, Arc2D.OPEN), false)
        }
        else {
          append(Arc2D.Double(oldXD - d, levelYD - d, d, d, 270.0, 90.0, Arc2D.OPEN), false)
          moveTo(oldXD - d / 2, levelYD)
          horizontalSegmentWithInterruptions(levelYD, newXD + d / 2, oldXD - d / 2, connectors)
          append(Arc2D.Double(newXD, levelYD, d, d, 90.0, 90.0, Arc2D.OPEN), false)
        }
        moveTo(newXD, levelYD + d / 2)
        lineTo(newXD, newSignatureTop.toDouble())
      })
    }

    private fun drawVerticalConnection(x: Int) {
      g?.draw(GeneralPath().apply {
        moveTo(x.toDouble(), oldSignatureBottom.toDouble())
        lineTo(x.toDouble(), newSignatureTop.toDouble())
      })
    }

    private fun GeneralPath.horizontalSegmentWithInterruptions(
      y: Double,
      minX: Double,
      maxX: Double,
      connectors: List<VerticalConnectorData>
    ) {
      if (minX >= maxX) return

      val interruptions = connectors
        .filter { minX < it.x && it.x < maxX && it.minY < y && y < it.maxY }
        .map { it.x }
        .sorted()

      var x = minX
      moveTo(x, y)
      for (interruption in interruptions) {
        lineTo((interruption - hLineInterruptionGap - connectionLineThickness).toDouble(), y)
        x = (interruption + hLineInterruptionGap + connectionLineThickness).toDouble()
        moveTo(x, y)
      }
      lineTo(maxX, y)
    }

    private data class ConnectionData(val oldX: Int, val newX: Int)

    private val ConnectionData.minX get() = min(oldX, newX)
    private val ConnectionData.maxX get() = max(oldX, newX)

    private fun ConnectionData.overlapsHorizontally(other: ConnectionData): Boolean {
      if (minX > other.maxX) return false
      if (maxX < other.minX) return false
      return true
    }

    private data class VerticalConnectorData(val x: Int, val minY: Int, val maxY: Int)
  }

  private class VerticalModeConnectionRouter(
    private val g: Graphics2D?,
    private val newSignatureLeft: Int,
    private val oldSignatureRight: Int
  ) : ConnectionRouter {

    override fun drawConnections(connectedBounds: List<Pair<Rectangle, Rectangle>>) {
      for ((oldBounds, newBounds) in connectedBounds) {
        drawConnection(oldBounds, newBounds)
      }
    }

    private fun drawConnection(oldBounds: Rectangle, newBounds: Rectangle) {
      val oldY = oldBounds.top + oldBounds.height.toDouble() / 2
      val newY = newBounds.top + newBounds.height.toDouble() / 2

      val p1 = Point2D.Double((oldSignatureRight + betweenSignaturesConnectionStraightPart).toDouble(), oldY)
      val p2 = Point2D.Double((newSignatureLeft - betweenSignaturesConnectionStraightPart).toDouble(), newY)

      val angle = atan((p1.y - p2.y) / (p2.x - p1.x))
      val angleInDegrees = angle * 180 / PI
      val radius = verticalModeArcW / sin(angle.absoluteValue / 2)

      g?.draw(GeneralPath().apply {
        moveTo(oldBounds.right.toDouble(), oldY)
        if (angle >= 0) {
          connectedArc(p1.x - verticalModeArcW, p1.y - radius, 270.0, angleInDegrees, radius)
          connectedArc(p2.x + verticalModeArcW, p2.y + radius, 90.0 + angleInDegrees, -angleInDegrees, radius)
        }
        else {
          connectedArc(p1.x - verticalModeArcW, p1.y + radius, 90.0, angleInDegrees, radius)
          connectedArc(p2.x + verticalModeArcW, p2.y - radius, 270.0 + angleInDegrees, -angleInDegrees, radius)
        }
        lineTo(newBounds.left.toDouble(), newY)
      })
    }

    private fun GeneralPath.connectedArc(centerX: Double, centerY: Double, start: Double, extentWithSign: Double, radius: Double) {
      val arc = Arc2D.Double(
        centerX - radius,
        centerY - radius,
        radius * 2,
        radius * 2,
        if (extentWithSign > 0) start else start + extentWithSign,
        extentWithSign.absoluteValue,
        Arc2D.OPEN
      )
      val startPoint = if (extentWithSign > 0) arc.startPoint else arc.endPoint
      val endPoint = if (extentWithSign > 0) arc.endPoint else arc.startPoint
      lineTo(startPoint.x, startPoint.y)
      append(arc, false)
      moveTo(endPoint.x, endPoint.y)
    }
  }

  private fun lineHeight(context: FontRenderContext): Int {
    return font.getMaxCharBounds(context).height.ceilToInt()
  }

  private companion object Constants {
    const val indentInVerticalMode = 4

    // horizontal mode
    const val betweenSignaturesVSpace = 10
    const val betweenSignaturesVSpaceWithOneHSegment = 16
    const val betweenHSegmentsVSpace = 6
    const val betweenConnectorsHSpace = 10
    const val rectangularConnectionArcR = 4.0
    const val hLineInterruptionGap = 1
    const val minHSegmentLength = 25

    // vertical mode
    const val betweenSignaturesHSpace = 50
    const val betweenSignaturesConnectionStraightPart = 10
    const val verticalModeArcW = 4.0

    // both modes
    const val topSpace = 14
    const val bottomSpace = 18
    const val leftSpace = 0
    const val rightSpace = 6
    const val connectionLineThickness = 2
    const val backgroundGap = 1
  }
}

private inline val Rectangle.left: Int
  get() = x
private inline val Rectangle.top: Int
  get() = y
private val Rectangle.right: Int
  get() = x + width

private fun Double.ceilToInt(): Int = ceil(this).toInt()

