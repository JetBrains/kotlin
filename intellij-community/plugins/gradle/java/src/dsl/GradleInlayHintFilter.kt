// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.gradle.service.resolve.isGradleScript
import org.jetbrains.plugins.groovy.editor.GroovyInlayHintFilter

class GradleInlayHintFilter : GroovyInlayHintFilter {

  override fun shouldHideHints(element: PsiElement): Boolean {
    return element.containingFile.isGradleScript()
  }
}
