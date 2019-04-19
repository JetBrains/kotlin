// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.groovy.lang.resolve.api.Arguments
import org.jetbrains.plugins.groovy.lang.typing.GrCallTypeCalculator

class GradleProjectCallTypeCalculator : GrCallTypeCalculator {

  private val methodNames = setOf(
    "getProject",
    "getArtifacts",
    "getTasks",
    "getDependencies"
  )

  override fun getType(receiver: PsiType?, method: PsiMethod, arguments: Arguments?, context: PsiElement): PsiType? {
    if (receiver !is GradleProjectAwareType) return null
    if (method.containingClass?.qualifiedName == GRADLE_API_PROJECT && method.name in methodNames) {
      val returnType = method.returnType as? PsiClassType ?: return null
      return receiver.setType(returnType) // pass info about project to the next reference
    }
    return null
  }
}
