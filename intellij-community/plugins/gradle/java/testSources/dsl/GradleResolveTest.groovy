// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrNewExpression
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test
import org.junit.runners.Parameterized

import static com.intellij.psi.CommonClassNames.JAVA_UTIL_DATE

@CompileStatic
class GradleResolveTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Parameterized.Parameters(name = "with Gradle-{0}")
  static Collection<Object[]> data() {
    return [[BASE_GRADLE_VERSION] as Object[]]
  }

  @Test
  void resolveTest() {
    importProject('')
    new RunAll().append {
      'resolve date constructor'()
    } append {
      'resolve date constructor 2'()
    } run()
  }

  void 'resolve date constructor'() {
    doTest('<caret>new Date()') {
      def expression = elementUnderCaret(GrNewExpression)
      def results = expression.multiResolve(false)
      assert results.size() == 1
      def method = (PsiMethod)results[0].element
      assert method.constructor
      assert method.containingClass.qualifiedName == JAVA_UTIL_DATE
    }
  }

  void 'resolve date constructor 2'() {
    doTest('<caret>new Date(1l)') {
      def expression = elementUnderCaret(GrNewExpression)
      def results = expression.multiResolve(false)
      assert results.size() == 1
      def method = (PsiMethod)results[0].element
      assert method.constructor
      assert method.containingClass.qualifiedName == JAVA_UTIL_DATE
    }
  }
}
