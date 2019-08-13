// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.gradle.service.resolve.GradleDomainObjectProperty
import org.jetbrains.plugins.gradle.service.resolve.GradleExtensionProperty
import org.jetbrains.plugins.groovy.util.ExpressionTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_DISTRIBUTION
import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_FILE_COPY_SPEC

@CompileStatic
class GradleDistributionsTest extends GradleHighlightingBaseTest implements ExpressionTest {

  @Test
  void distributionsTest() {
    importProject("apply plugin: 'distribution'")
    new RunAll().append {
      'distributions container'()
    } append {
      'distributions call'()
    } append {
      'distributions closure delegate'()
    } append {
      'distribution via unqualified property reference'()
    } append {
      'distribution via unqualified method call'()
    } append {
      'distribution closure delegate in unqualified method call'()
    } append {
      'distribution member via unqualified method call closure delegate'()
    } append {
      'distribution via qualified property reference'()
    } append {
      'distribution via qualified method call'()
    } append {
      'distribution closure delegate in qualified method call'()
    } append {
      'distribution member via qualified method call closure delegate'()
    } append {
      'distribution contents closure delegate'()
    } run()
  }

  @Override
  protected List<String> getParentCalls() {
//    return []
    return super.getParentCalls() + 'buildscript'
  }

  void 'distributions container'() {
    def type = isGradleAtLeast("3.5") ? "org.gradle.api.NamedDomainObjectContainer<org.gradle.api.distribution.Distribution>"
                                      : "org.gradle.api.distribution.internal.DefaultDistributionContainer"
    doTest('<caret>distributions') {
      referenceExpressionTest(GradleExtensionProperty, type)
    }
  }

  void 'distributions call'() {
    def type = isGradleAtLeast("3.5") ? "org.gradle.api.NamedDomainObjectContainer<org.gradle.api.distribution.Distribution>"
                                      : "org.gradle.api.distribution.internal.DefaultDistributionContainer"
    doTest('<caret>distributions {}') {
      methodCallTest(PsiMethod, type)
    }
  }

  void 'distributions closure delegate'() {
    def type = isGradleAtLeast("3.5") ? "org.gradle.api.NamedDomainObjectContainer<org.gradle.api.distribution.Distribution>"
                                      : "org.gradle.api.distribution.internal.DefaultDistributionContainer"
    doTest('distributions { <caret> }') {
      closureDelegateTest(type, 1)
    }
  }

  void 'distribution via unqualified property reference'() {
    doTest('distributions { <caret>foo }') {
      referenceExpressionTest(GradleDomainObjectProperty, GRADLE_API_DISTRIBUTION)
    }
  }

  void 'distribution via unqualified method call'() {
    doTest('distributions { <caret>foo {} }') {
      methodCallTest(PsiMethod, GRADLE_API_DISTRIBUTION)
    }
  }

  void 'distribution closure delegate in unqualified method call'() {
    doTest('distributions { foo { <caret> } }') {
      closureDelegateTest(GRADLE_API_DISTRIBUTION, 1)
    }
  }

  void 'distribution member via unqualified method call closure delegate'() {
    doTest('distributions { foo { <caret>getBaseName() } }') {
      def method = resolveMethodTest(PsiMethod)
      assert method.containingClass.qualifiedName == GRADLE_API_DISTRIBUTION
    }
  }

  void 'distribution via qualified property reference'() {
    doTest('distributions.<caret>foo') {
      referenceExpressionTest(GradleDomainObjectProperty, GRADLE_API_DISTRIBUTION)
    }
  }

  void 'distribution via qualified method call'() {
    doTest('distributions.<caret>foo {}') {
      methodCallTest(PsiMethod, GRADLE_API_DISTRIBUTION)
    }
  }

  void 'distribution closure delegate in qualified method call'() {
    doTest('distributions.foo { <caret> }') {
      closureDelegateTest(GRADLE_API_DISTRIBUTION, 1)
    }
  }

  void 'distribution member via qualified method call closure delegate'() {
    doTest('distributions.foo { <caret>getBaseName() }') {
      def method = resolveMethodTest(PsiMethod)
      assert method.containingClass.qualifiedName == GRADLE_API_DISTRIBUTION
    }
  }

  void 'distribution contents closure delegate'() {
    doTest('distributions { foo { contents { <caret> } } }') {
      closureDelegateTest(GRADLE_API_FILE_COPY_SPEC, 1)
    }
  }
}
