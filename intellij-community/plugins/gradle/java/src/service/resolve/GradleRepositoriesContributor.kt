// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.patterns.StandardPatterns.or
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo

/**
 * Provides gradle MavenArtifactRepository DSL resolving contributor.
 *
 *  e.g.
 *  repositories {
 *     maven {
 *       url "http://snapshots.repository.codehaus.org/"
 *     }
 *  }
 *
 * @author Vladislav.Soroka
 */
class GradleRepositoriesContributor : GradleMethodContextContributor {

  companion object {
    val repositoriesClosure: GroovyClosurePattern = groovyClosure().inMethod(or(
      psiMethod(GRADLE_API_PROJECT, "repositories"),
      psiMethod(GRADLE_API_SCRIPT_HANDLER, "repositories"),
      psiMethod(GRADLE_API_PUBLISHING_EXTENSION, "repositories")
    ))
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    if (repositoriesClosure.accepts(closure)) {
      return DelegatesToInfo(TypesUtil.createType(GRADLE_API_REPOSITORY_HANDLER, closure), Closure.DELEGATE_FIRST)
    }
    return null
  }
}
