// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.util.ExpressionTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleIdeaPluginScriptContributor.*

@CompileStatic
class GradleIdeaPluginTest extends GradleHighlightingBaseTest implements ExpressionTest {

  @Test
  void test() {
    importProject("apply plugin: 'idea'")
    new RunAll().append {
      'idea closure delegate'()
    } append {
      'idea project closure delegate'()
    } append {
      'idea project ipr closure delegate'()
    } append {
      'idea module closure delegate'()
    } append {
      'idea module iml closure delegate'()
    } run()
  }

  void 'idea closure delegate'() {
    doTest('idea { <caret> }') {
      closureDelegateTest(IDEA_MODEL_FQN, 1)
    }
  }

  void 'idea project closure delegate'() {
    doTest('idea { project { <caret> } }') {
      closureDelegateTest(IDEA_PROJECT_FQN, 1)
    }
  }

  void 'idea project ipr closure delegate'() {
    doTest('idea { project { ipr { <caret> } } }') {
      closureDelegateTest(IDE_XML_MERGER_FQN, 1)
    }
  }

  void 'idea module closure delegate'() {
    doTest('idea { module { <caret> } }') {
      closureDelegateTest(IDEA_MODULE_FQN, 1)
    }
  }

  void 'idea module iml closure delegate'() {
    doTest('idea { module { iml { <caret> } } }') {
      closureDelegateTest(IDEA_MODULE_IML_FQN, 1)
    }
  }
}
