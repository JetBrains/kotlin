// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.gradle.service.resolve.GradleTaskProperty
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection
import org.jetbrains.plugins.groovy.codeInspection.bugs.GroovyAccessibilityInspection
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection
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
    } append {
      'task declaration with unresolved identifier'()
    } append {
      'task declaration with unresolved identifier invalid'()
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

  void 'task declaration with unresolved identifier'() {
    doHighlightingTest '''\
task(id1)
task(id2) {}
task(id3, {})
task(id4, description: 'oh')
task(id5, description: 'oh') {}
task(id6, description: 'oh', {})

task id7()
task id8 {}
task id9() {}
task id10({})

task id11(description: 'hi')
task id12(description: 'hi') {}
task id13(description: 'hi', {})

task id14 << {}
''', GrUnresolvedAccessInspection, GroovyAssignabilityCheckInspection, GroovyAccessibilityInspection
  }

  void 'task declaration with unresolved identifier invalid'() {
    doHighlightingTest '''\
task <warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(java.lang.String, java.lang.Integer)'">id1, 42</warning>
task <warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(java.lang.Integer, ?)'">42, <warning descr="Cannot resolve symbol 'id2'">id2</warning></warning>
task <warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(java.lang.String, java.lang.Integer, java.lang.Integer)'">id3, 42, 43</warning>
task <warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(?, java.lang.Integer, java.lang.Integer, java.lang.Integer)'"><warning descr="Cannot resolve symbol 'id4'">id4</warning>, 42, 43, 69</warning>
task <warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(['description':java.lang.String], java.lang.String, java.lang.Integer)'">id5, description: 'a', 43</warning>
task <warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(['description':java.lang.String], ?, java.lang.Integer, java.lang.Integer)'"><warning descr="Cannot resolve symbol 'id6'">id6</warning>, description: 'a', 43, 69</warning>

task <weak_warning descr="Cannot infer argument types"><warning descr="Cannot resolve symbol 'id7'">id7</warning>(42)</weak_warning>
task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(groovy.lang.Closure<java.lang.Void>, ?)'">({}, <warning descr="Cannot resolve symbol 'id8'">id8</warning>)</warning>

task id9 + {}
''', GrUnresolvedAccessInspection, GroovyAssignabilityCheckInspection, GroovyAccessibilityInspection
  }
}
