// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui

import com.intellij.navigation.TargetPresentation
import com.intellij.openapi.project.Project
import com.intellij.ui.list.LeftRightSearchAwareRenderer
import com.intellij.ui.speedSearch.SearchAwareRenderer
import org.jetbrains.annotations.ApiStatus.Experimental

@Experimental
fun <T> createTargetPresentationRenderer(project: Project, presentation: (T) -> TargetPresentation?): SearchAwareRenderer<T> {
  val mainRenderer = createMainRenderer(project, presentation)
  if (UISettings.instance.showIconInQuickNavigation) {
    val rightRenderer = createRightRenderer(presentation)
    return LeftRightSearchAwareRenderer(mainRenderer, rightRenderer)
  }
  else {
    return mainRenderer
  }
}

private fun <T> createMainRenderer(project: Project, presentation: (T) -> TargetPresentation?): SearchAwareRenderer<T> {
  return object : TargetPresentationMainRenderer<T>(project) {
    override fun getPresentation(value: T): TargetPresentation? = presentation(value)
  }
}

private fun <T> createRightRenderer(presentation: (T) -> TargetPresentation?): SearchAwareRenderer<T> {
  return object : TargetPresentationRightRenderer<T>() {
    override fun getPresentation(value: T): TargetPresentation? = presentation(value)
  }
}
