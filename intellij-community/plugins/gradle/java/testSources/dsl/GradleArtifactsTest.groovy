// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_ARTIFACT_HANDLER
import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT

@CompileStatic
class GradleArtifactsTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Override
  protected List<String> getParentCalls() {
    return []
  }

  @Test
  void artifactsTest() {
    importProject("apply plugin: 'java'")
    new RunAll().append {
      'closure delegate'()
    } append {
      'member'()
    } append {
      'unresolved reference'()
    } append {
      'unresolved configuration reference'()
    } append {
      'invalid artifact addition'()
    } append {
      'artifact addition'()
    } append {
      'configurable artifact addition'()
    } append {
      'configuration delegate'()
    } append {
      'configuration delegate method setter'()
    } run()
  }

  void 'closure delegate'() {
    doTest('artifacts { <caret> }') {
      closureDelegateTest(GRADLE_API_ARTIFACT_HANDLER, 1)
    }
  }

  void 'member'() {
    doTest('artifacts { <caret>add("conf", "notation") }') {
      methodTest(resolveTest(PsiMethod), "add", GRADLE_API_ARTIFACT_HANDLER)
    }
  }

  void 'unresolved reference'() {
    doTest('artifacts { <caret>foo }', super.getParentCalls()) {
      resolveTest(null)
    }
  }

  void 'unresolved configuration reference'() {
    doTest('artifacts { <caret>compile }') {
      resolveTest(null)
    }
  }

  void 'invalid artifact addition'() {
    // foo configuration doesn't exist
    doTest('artifacts { <caret>foo("artifactNotation") }') {
      assertEmpty(elementUnderCaret(GrMethodCall).multiResolve(false))
    }
  }

  void 'artifact addition'() {
    def test = {
      def call = elementUnderCaret(GrMethodCall)
      def result = assertOneElement(call.multiResolve(false))
      methodTest(assertInstanceOf(result.element, PsiMethod), "compile", GRADLE_API_ARTIFACT_HANDLER)
      assert result.applicable
      assert call.type == PsiType.NULL
    }
    doTest('artifacts { <caret>compile("artifactNotation") }', test)
    doTest('artifacts { <caret>compile("artifactNotation", "artifactNotation2", "artifactNotation3") }', test)
    doTest('artifacts.<caret>compile("artifactNotation")', test)
    doTest('artifacts.<caret>compile("artifactNotation", "artifactNotation2", "artifactNotation3")', test)
  }

  void 'configurable artifact addition'() {
    def test = {
      def call = elementUnderCaret(GrMethodCall)
      def result = assertOneElement(call.multiResolve(false))
      methodTest(assertInstanceOf(result.element, PsiMethod), "compile", GRADLE_API_ARTIFACT_HANDLER)
      assert result.applicable
      assert call.type.equalsToText(GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT)
    }
    doTest('artifacts { <caret>compile("artifactNotation") {} }', test)
    doTest('artifacts.<caret>compile("artifactNotation") {}', test)
  }

  void 'configuration delegate'() {
    doTest('artifacts { compile("artifactNotation") { <caret> } }') {
      closureDelegateTest(GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT, 1)
    }
  }

  void 'configuration delegate method setter'() {
    doTest('artifacts { compile("artifactNotation") { <caret>name("hi") } }') {
      setterMethodTest('name', 'setName', GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT)
    }
  }
}
