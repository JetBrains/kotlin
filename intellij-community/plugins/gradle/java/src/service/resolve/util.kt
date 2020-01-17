// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.openapi.util.Key
import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.gradle.util.GradleConstants.EXTENSION
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyMethodResult
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass

val projectTypeKey: Key<GradleProjectAwareType> = Key.create("gradle.current.project")
val saveProjectType: PatternCondition<GroovyMethodResult> = object : PatternCondition<GroovyMethodResult>("saveProjectContext") {
  override fun accepts(result: GroovyMethodResult, context: ProcessingContext?): Boolean {
    // Given the closure matched some method,
    // we want to determine what we know about this Project.
    // This PatternCondition just saves the info into the ProcessingContext.
    context?.put(projectTypeKey, result.candidate?.receiverType as? GradleProjectAwareType)
    return true
  }
}

val DELEGATED_TYPE: Key<Boolean> = Key.create("gradle.delegated.type")

/**
 * @author Vladislav.Soroka
 */
internal fun PsiClass?.isResolvedInGradleScript() = this is GroovyScriptClass && this.containingFile.isGradleScript()

internal fun PsiFile?.isGradleScript() = this?.originalFile?.virtualFile?.extension == EXTENSION
