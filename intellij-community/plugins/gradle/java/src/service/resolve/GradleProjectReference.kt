// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.model.SingleTargetReference
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiCompletableReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleProject
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral

@Internal
class GradleProjectReference(
  private val myElement: GrLiteral,
  private val myRange: TextRange,
  private val myQualifiedName: List<String>
) : SingleTargetReference(), PsiCompletableReference {

  override fun getElement(): PsiElement = myElement

  override fun getRangeInElement(): TextRange = myRange

  override fun resolveSingleTarget(): Symbol? {
    val gradleProject = GradleExtensionsSettings.getRootProject(myElement) ?: return null
    val rootProjectPath = GradleExtensionsSettings.getRootProjectPath(myElement) ?: return null
    if (GradleProjectSymbol.qualifiedName(myQualifiedName) in gradleProject.extensions) {
      return GradleProjectSymbol(myQualifiedName, rootProjectPath)
    }
    return null
  }

  /**
   * This could've been much easier if we could query list of sub-projects by project fqn.
   */
  override fun getCompletionVariants(): Collection<Any> {
    val gradleProject: GradleProject = GradleExtensionsSettings.getRootProject(myElement) ?: return emptyList()
    val parentProjectFqn: List<String> = myQualifiedName.dropLast(1) // ["com", "foo", "IntellijIdeaRulezzz "] -> ["com", "foo"]
    val parentProjectPrefix: String = parentProjectFqn.joinToString(separator = "", postfix = ":") { ":$it" } // ":com:foo:"
    val result = LinkedHashSet<String>()
    for (projectFqn in gradleProject.extensions.keys) { // let's say there is ":com:foo:bar:baz" among keys
      if (!projectFqn.startsWith(parentProjectPrefix)) {
        continue
      }
      val relativeFqn = projectFqn.removePrefix(parentProjectPrefix) // "bar:baz"
      val childProjectName = relativeFqn.split(':').firstOrNull() // "bar"
      if (childProjectName.isNullOrEmpty()) {
        continue
      }
      result += childProjectName
    }
    return result
  }
}
