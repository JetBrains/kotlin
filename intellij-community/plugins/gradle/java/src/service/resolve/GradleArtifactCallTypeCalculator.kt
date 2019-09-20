// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder.checkKind
import org.jetbrains.plugins.groovy.lang.resolve.api.Arguments
import org.jetbrains.plugins.groovy.lang.resolve.api.ExpressionArgument
import org.jetbrains.plugins.groovy.lang.typing.GrCallTypeCalculator

class GradleArtifactCallTypeCalculator : GrCallTypeCalculator {

  override fun getType(receiver: PsiType?, method: PsiMethod, arguments: Arguments?, context: PsiElement): PsiType? {
    if (!checkKind(method, GradleArtifactHandlerContributor.ourMethodKind)) return null
    if (arguments != null && isConfigurableArtifact(arguments)) {
      return createType(GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT, context)
    }
    else {
      return PsiType.NULL
    }
  }

  private fun isConfigurableArtifact(arguments: Arguments): Boolean {
    if (arguments.size != 2) return false
    val argument = arguments[1] as? ExpressionArgument ?: return false
    return argument.expression is GrClosableBlock
  }
}
