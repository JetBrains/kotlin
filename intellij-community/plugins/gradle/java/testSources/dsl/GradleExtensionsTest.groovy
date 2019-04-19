// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyProperty
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

@CompileStatic
class GradleExtensionsTest extends GradleHighlightingBaseTest implements ResolveTest {

  protected List<String> getParentCalls() {
    // todo resolve extensions also for non-root places
    return []
  }

  @Test
  void extensionsTest() {
    importProject("")
    new RunAll().append {
      "project level extension property"()
    } append {
      "project level extension call type"()
    } append {
      "project level extension closure delegate type"()
    } run()
  }

  void "project level extension property"() {
    doTest("ext") {
      def ref = elementUnderCaret(GrReferenceExpression)
      assert ref.resolve() instanceof GroovyProperty
      assert ref.type.equalsToText(getExtraPropertiesExtensionFqn())
    }
  }

  void "project level extension call type"() {
    doTest("ext {}") {
      def call = elementUnderCaret(GrMethodCallExpression)
      assert call.resolveMethod() instanceof GrMethod
      assert call.type.equalsToText(getExtraPropertiesExtensionFqn())
    }
  }

  void "project level extension closure delegate type"() {
    doTest("ext {<caret>}") {
      closureDelegateTest(getExtraPropertiesExtensionFqn(), 1)
    }
  }
}
