// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.gradle.service.resolve.GradleTaskProperty
import org.jetbrains.plugins.groovy.util.ExpressionTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*

@CompileStatic
class GradleTasksTest extends GradleHighlightingBaseTest implements ExpressionTest {

  @Override
  protected List<String> getParentCalls() {
    return []
  }

  @Test
  void test() {
    importProject("apply plugin:'java'")
    new RunAll().append {
      'task via script'()
    } append {
      'task container vs task'()
    } append {
      'task via TaskContainer'()
    } append {
      'task via Project'()
    } append {
      'task in allProjects'()
    } append {
      'task in allProjects via explicit delegate'()
    } run()
  }

  void 'task via script'() {
    doTest('<caret>javadoc') {
      testTask('javadoc', GRADLE_API_TASKS_JAVADOC_JAVADOC)
    }
  }

  void 'task container vs task'() {
    doTest('<caret>tasks') {
      referenceExpressionTest(PsiMethod, GRADLE_API_TASK_CONTAINER)
    }
  }

  void 'task via TaskContainer'() {
    doTest('tasks.<caret>tasks') {
      testTask('tasks', GRADLE_API_TASKS_DIAGNOSTICS_TASK_REPORT_TASK)
    }
  }

  void 'task via Project'() {
    doTest('project.<caret>clean') {
      testTask('clean', GRADLE_API_TASKS_DELETE)
    }
  }

  void 'task in allProjects'() {
    doTest('allProjects { <caret>clean }') {
      testTask('clean', GRADLE_API_TASKS_DELETE)
    }
  }

  void 'task in allProjects via explicit delegate'() {
    doTest('allProjects { delegate.<caret>clean }') {
      resolveTest(null)
    }
  }

  private void testTask(String name, String type) {
    def property = referenceExpressionTest(GradleTaskProperty, type)
    assert property.name == name
  }
}
