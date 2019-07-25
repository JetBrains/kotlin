/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.codeInsight

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.patterns.StandardPatterns.string
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl

/**
 * @author Vladislav.Soroka
 */
abstract class AbstractGradleCompletionContributor : CompletionContributor() {

  protected fun findNamedArgumentValue(namedArgumentsOwner: GrNamedArgumentsOwner?, label: String): String? {
    val namedArgument = namedArgumentsOwner?.findNamedArgument(label) ?: return null
    return (namedArgument.expression as? GrLiteralImpl)?.value?.toString()
  }

  companion object {
    val GRADLE_FILE_PATTERN: ElementPattern<PsiElement> = psiElement()
      .inFile(psiFile().withName(string().endsWith('.' + GradleConstants.EXTENSION)))
  }
}
