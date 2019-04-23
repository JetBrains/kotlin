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
      'task ref'()
    } append {
      'task call'()
    } append {
      'task call delegate'()
    } append {
      'task container vs task ref'()
    } append {
      'task container vs task call'()
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

  void 'task ref'() {
    doTest('<caret>javadoc') {
      testTask('javadoc', GRADLE_API_TASKS_JAVADOC_JAVADOC)
    }
  }

  void 'task call'() {
    doTest('<caret>javadoc {}') {
      methodCallTest(PsiMethod, GRADLE_API_TASKS_JAVADOC_JAVADOC)
    }
  }

  void 'task call delegate'() {
    doTest('javadoc { <caret> }') {
      closureDelegateTest(GRADLE_API_TASKS_JAVADOC_JAVADOC, 1)
    }
  }

  void 'task container vs task ref'() {
    doTest('<caret>tasks') {
      referenceExpressionTest(PsiMethod, GRADLE_API_TASK_CONTAINER)
    }
  }

  void 'task container vs task call'() {
    doTest('<caret>tasks {}') {
      methodCallTest(PsiMethod, GRADLE_API_TASKS_DIAGNOSTICS_TASK_REPORT_TASK)
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
