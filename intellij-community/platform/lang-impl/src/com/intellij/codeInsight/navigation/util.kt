// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import java.util.regex.Pattern

private val CONTAINER_PATTERN: Pattern = Pattern.compile("(\\(in |\\()?([^)]*)(\\))?")

internal fun ItemPresentation.getColoredAttributes(): TextAttributes? {
  val coloredPresentation = this as? ColoredItemPresentation
  val textAttributesKey = coloredPresentation?.textAttributesKey ?: return null
  return EditorColorsManager.getInstance().schemeForCurrentUITheme.getAttributes(textAttributesKey)
}

internal fun ItemPresentation.getLocationText(): String? {
  val locationString = locationString ?: return null
  val matcher = CONTAINER_PATTERN.matcher(locationString)
  return if (matcher.matches()) matcher.group(2) else locationString
}
