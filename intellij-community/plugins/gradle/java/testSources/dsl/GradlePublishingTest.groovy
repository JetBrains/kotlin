// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiType
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_ARTIFACTS_REPOSITORIES_MAVEN_ARTIFACT_REPOSITORY

@CompileStatic
class GradlePublishingTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Test
  void repositoriesTest() {
    importProject("apply plugin: 'maven-publish'")
    new RunAll().append {
      'publishing closure delegate'()
    } append {
      'publishing repositories maven url'()
    } run()
  }

  @Override
  protected List<String> getParentCalls() {
    return super.getParentCalls() + 'buildscript'
  }

  void 'publishing closure delegate'() {
    doTest('publishing { <caret> }') {
      closureDelegateTest(getPublishingExtensionFqn(), 1)
    }
  }

  void 'publishing repositories maven url'() {
    doTest('publishing { repositories { maven { url<caret> "" } } }') {
      def call = elementUnderCaret(GrMethodCall)
      def method = call.resolveMethod()
      assert method != null
      assert method.containingClass.qualifiedName == GRADLE_API_ARTIFACTS_REPOSITORIES_MAVEN_ARTIFACT_REPOSITORY
      assert method.name == "setUrl"
      PsiType argType = method.parameters[0].type as PsiType
      assert argType.equalsToText(JAVA_LANG_OBJECT)
    }
  }

  private String getPublishingExtensionFqn() {
    isGradleOlderThen_4_8() || isGradleNewerOrSameThen_5_0() ? "org.gradle.api.publish.internal.DefaultPublishingExtension"
                                                             : "org.gradle.api.publish.internal.DeferredConfigurablePublishingExtension"
  }
}
