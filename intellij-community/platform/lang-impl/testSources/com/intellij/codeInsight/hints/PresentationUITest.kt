// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.*
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.PlatformTestUtil
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class PresentationUITest : LightPlatformCodeInsightTestCase() {
  val factory by lazy { PresentationFactory(editor as EditorImpl) }

  override fun setUp() {
    super.setUp()
    configureFromFileText("test.java", "class A{}")
  }

  fun getPath() : String {
    return PlatformTestUtil.getCommunityPath()
             .replace(File.separatorChar, '/') + "/platform/lang-impl/testData/editor/inlays/ui/"
  }

  fun testContainerPresentation() {
    val inner = ContainerInlayPresentation(SpacePresentation(50, 20), null, InlayPresentationFactory.RoundedCorners(10, 10), Color.BLUE)
    val presentation = ContainerInlayPresentation(inner, InlayPresentationFactory.Padding(3, 5, 6, 8), null, Color.RED)

    testPresentationAsExpected(presentation)
  }

  fun testVerticalContainerPresentation() {
    val vertical = VerticalListInlayPresentation(listOf(
      ContainerInlayPresentation(SpacePresentation(30, 10), null, null, Color.BLUE),
      ContainerInlayPresentation(SpacePresentation(15, 7), null, null, Color.RED),
      ContainerInlayPresentation(SpacePresentation(19, 12), null, null, Color.GREEN)
    ))
    testPresentationAsExpected(vertical)
  }


  /**
   * Send click to presentation
   * [x] and [y] must be in coordinates of presentation
   */
  private fun emulateClick(presentation: InlayPresentation, x: Int, y: Int) {
    presentation.mouseClicked(MouseEvent(null, 0, 0, 0, x, y, 1, false), Point(x, y))
  }


  private fun testPresentationAsExpected(presentation: InlayPresentation) {
    val width = presentation.width
    val height = presentation.height

    @Suppress("UndesirableClassUsage")
    val actual = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
    val graphics = actual.graphics
    presentation.paint(graphics as Graphics2D, TextAttributes())

    val expectedFilePath = getPath() + "/" + getTestName(false) + ".png"
    val file = File(expectedFilePath)
    if (!REPLACE_WITH_ACTUAL) {
      assertTrue(file.exists())
    } else {
      if (!file.exists()) {
        writeImage(actual, file)
        fail("No file found, created from actual")
      }
    }
    val expected = ImageIO.read(file)
    val imagesEqual = imagesEqual(actual, expected)
    if (REPLACE_WITH_ACTUAL) {
      writeImage(actual, file)
    }
    assertTrue("Images are different", imagesEqual)
  }

  private fun writeImage(actual: BufferedImage, file: File) {
    ImageIO.write(actual, "png", file)
  }

  private fun imagesEqual(img1: BufferedImage, img2: BufferedImage): Boolean {
    if (img1.width != img2.width || img1.height != img2.height) return false
    for (x in 0 until img1.width) {
      for (y in 0 until img1.height) {
        if (img1.getRGB(x, y) != img2.getRGB(x, y)) return false
      }
    }
    return true
  }

  companion object {
    var REPLACE_WITH_ACTUAL = false
  }
}