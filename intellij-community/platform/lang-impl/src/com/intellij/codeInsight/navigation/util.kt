// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
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

internal fun orderEntryText(fileIndex: ProjectFileIndex, file: VirtualFile): String? {
  val entry = fileIndex.getOrderEntriesForFile(file).find { entry ->
    entry is LibraryOrderEntry || entry is JdkOrderEntry
  }
  return entry?.presentableName
}

internal fun sdkText(file: VirtualFile): String? {
  if (!Registry.`is`("index.run.configuration.jre")) return null
  val sdk = ProjectJdkTable.getInstance().allJdks.find { sdk ->
    val rootProvider = sdk.rootProvider
    val roots = setOf(*rootProvider.getFiles(OrderRootType.CLASSES), *rootProvider.getFiles(OrderRootType.SOURCES))
    VfsUtilCore.isUnder(file, roots)
  }
  return sdk?.run { "< ${name} >" }
}
