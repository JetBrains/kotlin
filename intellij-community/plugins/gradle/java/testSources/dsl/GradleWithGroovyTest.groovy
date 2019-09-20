// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_DOMAIN_OBJECT_COLLECTION
import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import static org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_CLOSURE

@CompileStatic
class GradleWithGroovyTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Override
  protected List<String> getParentCalls() {
    return super.getParentCalls() + 'buildscript'
  }

  @Test
  void artifactsTest() {
    importProject("apply plugin: 'java'; dependencies { compile 'org.codehaus.groovy:groovy:2.5.6' }")
    new RunAll().append {
      'Project#allprojects call'()
    } append {
      'DomainObjectCollection#all call'()
    } append {
      'DomainObjectCollection#withType call'()
    } append {
      'DGM#collect'()
    } run()
  }

  void 'Project#allprojects call'() {
    doTest('<caret>allprojects {}') {
      def method = assertInstanceOf(assertOneElement(elementUnderCaret(GrMethodCall).multiResolve(false)).element, PsiMethod)
      assert method.containingClass.qualifiedName == GRADLE_API_PROJECT
      assert method.parameterList.parameters.first().type.equalsToText(GROOVY_LANG_CLOSURE)
    }
  }

  void 'DomainObjectCollection#all call'() {
    doTest('<caret>configurations.all {}') {
      def method = assertInstanceOf(assertOneElement(elementUnderCaret(GrMethodCall).multiResolve(false)).element, PsiMethod)
      assert method.containingClass.qualifiedName == GRADLE_API_DOMAIN_OBJECT_COLLECTION
      assert method.parameterList.parameters.first().type.equalsToText(GROOVY_LANG_CLOSURE)
    }
  }

  void 'DomainObjectCollection#withType call'() {
    doTest('<caret>plugins.withType(JavaPlugin) {}') {
      def method = assertInstanceOf(assertOneElement(elementUnderCaret(GrMethodCall).multiResolve(false)).element, PsiMethod)
      assert method.containingClass.qualifiedName == GRADLE_API_DOMAIN_OBJECT_COLLECTION
      assert method.parameterList.parameters.last().type.equalsToText(GROOVY_LANG_CLOSURE)
    }
  }

  void 'DGM#collect'() {
    fixture.enableInspections(GroovyAssignabilityCheckInspection)
    testHighlighting '''["a", "b"].collect { it.toUpperCase() }'''
  }
}
