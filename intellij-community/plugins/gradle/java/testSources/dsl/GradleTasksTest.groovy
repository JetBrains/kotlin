// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.gradle.service.resolve.GradleTaskProperty
import org.jetbrains.plugins.groovy.util.ExpressionTest
import org.junit.Test

import static com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
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
    } append {
      'task declaration configuration delegate'()
    } append {
      'task declaration configuration delegate with explicit type'()
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

  void 'task declaration configuration delegate'() {
    def data = [
      "task('s') { <caret> }",
      "task(id2) { <caret> }",
      "task(id3, { <caret> })",
      "task(id5, description: 'oh') { <caret> }",
      "task(id6, description: 'oh', { <caret> })",
      "task id9() { <caret>}",
      "task id8 { <caret> }",
      "task id10({ <caret> })",
      "task id12(description: 'hi') { <caret> }",
      "task id13(description: 'hi', { <caret> })",
      "task mid12([description: 'hi']) { <caret> }",
      "task mid13([description: 'hi'], { <caret> })",
      "task emid12([:]) { <caret> }",
      "task emid13([:], { <caret> })",
      "tasks.create(name: 'cid1') { <caret> }",
      "tasks.create([name: 'mcid1']) { <caret> }",
      "tasks.create('eid1') { <caret> }",
    ]
    doTest(data) {
      closureDelegateTest(GRADLE_API_TASK, 1)
    }
  }

  void 'task declaration configuration delegate with explicit type'() {
    def data = [
      "task('s', type: String) { <caret> }",
      "task(id5, type: String) { <caret> }",
      "task(id6, type: String, { <caret> })",
      "task id12(type: String) { <caret> }",
      "task id13(type: String, { <caret> })",
      "task mid12([type: String]) { <caret> }",
      "task mid13([type: String], { <caret> })",
      "tasks.create(name: 'cid1', type: String) { <caret> }",
      "tasks.create([name: 'mcid1', type: String]) { <caret> }",
      "tasks.create('eid1', String) { <caret> }",
    ]
    doTest(data) {
      closureDelegateTest(JAVA_LANG_STRING, 1)
    }
  }
}
