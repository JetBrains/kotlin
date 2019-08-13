// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT

@CompileStatic
class GradleProjectTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Test
  void resolveTest() {
    importProject('')
    new RunAll().append {
      'resolve explicit getter'()
    } append {
      'resolve property'()
    } append {
      'resolve explicit setter'()
    } append {
      'resolve explicit setter without argument'()
    } append {
      'resolve property setter'()
    } append {
      'resolve implicit setter'()
    } append {
      'resolve implicit setter without argument'()
    } append {
      'property vs task'()
    } run()
  }

  void 'resolve explicit getter'() {
    doTest('<caret>getGroup()') {
      def results = elementUnderCaret(GrMethodCall).multiResolve(false)
      assert results.size() == 1
      def method = assertInstanceOf(results[0].element, PsiMethod)
      assert method.name == 'getGroup'
      assert method.containingClass.qualifiedName == GRADLE_API_PROJECT
    }
  }

  void 'resolve property'() {
    doTest('<caret>group') {
      def results = elementUnderCaret(GrReferenceExpression).multiResolve(false)
      assert results.size() == 1
      def method = assertInstanceOf(results[0].element, PsiMethod)
      assert method.name == 'getGroup'
      assert method.containingClass.qualifiedName == GRADLE_API_PROJECT
    }
  }

  void 'resolve explicit setter'() {
    doTest('<caret>setGroup(1)') {
      def results = elementUnderCaret(GrMethodCall).multiResolve(false)
      assert results.size() == 1
      def method = assertInstanceOf(results[0].element, PsiMethod)
      assert method.name == 'setGroup'
      assert method.containingClass.qualifiedName == GRADLE_API_PROJECT
    }
  }

  void 'resolve explicit setter without argument'() {
    doTest('<caret>setGroup()') {
      def results = elementUnderCaret(GrMethodCall).multiResolve(false)
      assert results.size() == 1
      def method = assertInstanceOf(results[0].element, PsiMethod)
      assert method.name == 'setGroup'
      assert method.containingClass.qualifiedName == GRADLE_API_PROJECT
    }
  }

  void 'resolve property setter'() {
    doTest('<caret>group = 42') {
      def results = elementUnderCaret(GrReferenceExpression).multiResolve(false)
      assert results.size() == 1
      def method = assertInstanceOf(results[0].element, PsiMethod)
      assert method.name == 'setGroup'
      assert method.containingClass.qualifiedName == GRADLE_API_PROJECT
    }
  }

  void 'resolve implicit setter'() {
    doTest('<caret>group(42)') {
      setterMethodTest('group', 'setGroup', GRADLE_API_PROJECT)
    }
  }

  void 'resolve implicit setter without argument'() {
    doTest('<caret>group()') {
      setterMethodTest('group', 'setGroup', GRADLE_API_PROJECT)
    }
  }

  @CompileDynamic
  void 'property vs task'() {
    doTest('<caret>dependencies') {
      methodTest(resolveTest(PsiMethod), "getDependencies", GRADLE_API_PROJECT)
    }
  }
}
