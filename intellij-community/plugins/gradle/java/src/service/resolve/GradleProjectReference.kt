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
  private val literal: GrLiteral,
  private val range: TextRange,
  private val projectPath: List<String>
) : SingleTargetReference(), PsiCompletableReference {

  init {
    require(projectPath.isNotEmpty())
  }

  private val projectPathString = projectPath.joinToString(":")

  override fun getElement(): PsiElement = literal

  override fun getRangeInElement(): TextRange = range

  override fun resolveSingleTarget(): Symbol? {
    val gradleProject = GradleExtensionsSettings.getRootProject(literal) ?: return null
    val rootProjectPath = GradleExtensionsSettings.getRootProjectPath(literal) ?: return null
    if (projectPathString in gradleProject.extensions) {
      return GradleProjectSymbol(projectPath, rootProjectPath)
    }
    return null
  }

  /**
   * This could be much easier if we could query list of sub-projects by project fqn.
   */
  override fun getCompletionVariants(): Collection<Any> {
    val gradleProject: GradleProject = GradleExtensionsSettings.getRootProject(literal) ?: return emptyList()
    val allProjectFqns: Set<String> = gradleProject.extensions.keys
    val parentProjectFqn: List<String> = projectPath // ["com", "IntellijIdeaRulezzz"]
      .run { take(size - 1) } // ["com"]
    val parentProjectPrefix: String = parentProjectFqn.joinToString("") { "$it:" } // "com:"
    val result = LinkedHashSet<String>()
    for (projectFqn in allProjectFqns) { // "com:foo:bar"
      if (!projectFqn.startsWith(parentProjectPrefix)) {
        continue
      }
      val relativeFqn = projectFqn.removePrefix(parentProjectPrefix) // "foo:bar"
      val childProjectName = relativeFqn.split(':').firstOrNull() // "foo"
      if (childProjectName.isNullOrEmpty()) {
        continue
      }
      result += childProjectName
    }
    return result
  }
}
