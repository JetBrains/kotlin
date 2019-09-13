// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("RunAnythingContextUtils")

package com.intellij.ide.actions.runAnything

import com.intellij.ide.actions.runAnything.RunAnythingContext.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.rootManager

fun RunAnythingContext.getPath() = when (this) {
  is ProjectContext -> project.guessProjectDir()?.path
  is ModuleContext -> module.guessModuleDir()?.path
  is RecentDirectoryContext -> path
  is BrowseRecentDirectoryContext -> null
}

private fun Module.guessModuleDir() =
  rootManager.contentRoots.firstOrNull { it.isDirectory }
