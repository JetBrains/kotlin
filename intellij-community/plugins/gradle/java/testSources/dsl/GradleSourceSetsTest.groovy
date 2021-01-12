// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_SOURCE_SET
import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_SOURCE_SET_CONTAINER

@CompileStatic
class GradleSourceSetsTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Override
  protected List<String> getParentCalls() {
    return super.getParentCalls() + 'buildscript'
  }

  @Test
  void sourceSetsTest() {
    importProject("apply plugin: 'java'")
    new RunAll().append {
      'sourceSets closure delegate'()
    } append {
      'source set via unqualified property reference'()
    } append {
      'source set via unqualified method call'()
    } append {
      'source set closure delegate in unqualified method call'()
    } append {
      'source set member via unqualified method call closure delegate'()
    } append {
      'source set via qualified property reference'()
    } append {
      'source set via qualified method call'()
    } append {
      'source set closure delegate in qualified method call'()
    } append {
      'source set member via qualified method call closure delegate'()
    } run()
  }

  void 'sourceSets closure delegate'() {
    doTest('sourceSets { <caret> }') {
      closureDelegateTest(GRADLE_API_SOURCE_SET_CONTAINER, 1)
    }
  }

  void 'source set via unqualified property reference'() {
    doTest('sourceSets { <caret>main }') {
      def ref = elementUnderCaret(GrReferenceExpression)
      assert ref.resolve() != null
      assert ref.type.equalsToText(GRADLE_API_SOURCE_SET)
    }
  }

  void 'source set via unqualified method call'() {
    doTest('sourceSets { <caret>main {} }') {
      def call = elementUnderCaret(GrMethodCall)
      assert call.resolveMethod() != null
      assert call.type.equalsToText(GRADLE_API_SOURCE_SET)
    }
  }

  void 'source set closure delegate in unqualified method call'() {
    doTest('sourceSets { main { <caret> } }') {
      closureDelegateTest(GRADLE_API_SOURCE_SET, 1)
    }
  }

  void 'source set member via unqualified method call closure delegate'() {
    doTest('sourceSets { main { <caret>getJarTaskName() } }') {
      def call = elementUnderCaret(GrMethodCall)
      def method = call.resolveMethod()
      assert method != null
      assert method.containingClass.qualifiedName == GRADLE_API_SOURCE_SET
    }
  }

  void 'source set via qualified property reference'() {
    doTest('sourceSets.<caret>main') {
      def ref = elementUnderCaret(GrReferenceExpression)
      assert ref.resolve() != null
      assert ref.type.equalsToText(GRADLE_API_SOURCE_SET)
    }
  }

  void 'source set via qualified method call'() {
    doTest('sourceSets.<caret>main {}') {
      def call = elementUnderCaret(GrMethodCall)
      assert call.resolveMethod() != null
      assert call.type.equalsToText(GRADLE_API_SOURCE_SET)
    }
  }

  void 'source set closure delegate in qualified method call'() {
    doTest('sourceSets.main { <caret> }') {
      closureDelegateTest(GRADLE_API_SOURCE_SET, 1)
    }
  }

  void 'source set member via qualified method call closure delegate'() {
    doTest('sourceSets.main { <caret>getJarTaskName() }') {
      def call = elementUnderCaret(GrMethodCall)
      def method = call.resolveMethod()
      assert method != null
      assert method.containingClass.qualifiedName == GRADLE_API_SOURCE_SET
    }
  }
}
