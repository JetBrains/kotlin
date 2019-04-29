// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_JAVA_PLUGIN_CONVENTION

@CompileStatic
class GradleConventionsTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Test
  void test() {
    importProject("apply plugin: 'java'")
    new RunAll().append {
      'property read'()
    } append {
      'property read via project'()
    } append {
      'property write'()
    } append {
      'setter method'()
    } run()
  }

  void 'property read'() {
    doTest('<caret>docsDir') {
      methodTest(resolveTest(PsiMethod), 'getDocsDir', GRADLE_API_JAVA_PLUGIN_CONVENTION)
    }
  }

  void 'property read via project'() {
    doTest('project.<caret>docsDir') {
      methodTest(resolveTest(PsiMethod), 'getDocsDir', GRADLE_API_JAVA_PLUGIN_CONVENTION)
    }
  }

  void 'property write'() {
    doTest('<caret>sourceCompatibility = 42') {
      methodTest(resolveTest(PsiMethod), 'setSourceCompatibility', GRADLE_API_JAVA_PLUGIN_CONVENTION)
    }
  }

  // this test is wrong and exists only to preserve current behaviour and to fail when behaviour changes
  void 'setter method'() {
    doTest('<caret>targetCompatibility("1.8")') {
      setterMethodTest('targetCompatibility', 'setTargetCompatibility', GRADLE_API_JAVA_PLUGIN_CONVENTION)
//      // the correct test is below:
//      def call = elementUnderCaret(GrMethodCall)
//      def result = call.advancedResolve()
//      assert result.invokedOnProperty
//      // getTargetCompatibility() should be resolved, just because it exists, but later it's highlighted with warning
//      methodTest(assertInstanceOf(result.element, PsiMethod), 'getTargetCompatibility', GRADLE_API_JAVA_PLUGIN_CONVENTION)
    }
  }
}
