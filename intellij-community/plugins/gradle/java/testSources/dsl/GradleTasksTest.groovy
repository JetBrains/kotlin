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
def t1 = task(idt1)
def t2 = task(idt2, {})
def t2_ = task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(groovy.lang.Closure<java.lang.Void>, ?)'">({}, <warning descr="Cannot resolve symbol 'idt2_'">idt2_</warning>)</warning>
def t3 = task(description: 'oh', idt3)
def t4 = task(description: 'hi', idt4, {})
def t4_ = task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(['description':java.lang.String], groovy.lang.Closure<java.lang.Void>, ?)'">(description: 'mark', {}, <warning descr="Cannot resolve symbol 'idt4_'">idt4_</warning>)</warning>

def insideClosure = {
    def ct1 = task(cidt1)
    def ct2 = task(cidt2, {})
    def ct2_ = task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(groovy.lang.Closure<java.lang.Void>, ?)'">({}, <warning descr="Cannot resolve symbol 'cidt2_'">cidt2_</warning>)</warning>
    def ct3 = task(description: 'oh', cidt3)
    def ct4 = task(description: 'hi', cidt4, {})
    def ct4_ = task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(['description':java.lang.String], groovy.lang.Closure<java.lang.Void>, ?)'">(description: 'mark', {}, <warning descr="Cannot resolve symbol 'cidt4_'">cidt4_</warning>)</warning>
}
insideClosure()

def insideMethod() {
    def mt1 = task(midt1)
    def mt2 = task(midt2, {})
    def mt2_ = task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(groovy.lang.Closure<java.lang.Void>, ?)'">({}, <warning descr="Cannot resolve symbol 'midt2_'">midt2_</warning>)</warning>
    def mt3 = task(description: 'oh', midt3)
    def mt4 = task(description: 'hi', midt4, {})
    def mt4_ = task<warning descr="'task' in 'org.gradle.api.Project' cannot be applied to '(['description':java.lang.String], groovy.lang.Closure<java.lang.Void>, ?)'">(description: 'mark', {}, <warning descr="Cannot resolve symbol 'midt4_'">midt4_</warning>)</warning>
}
insideMethod()

tasks.each {
    println it
}

project.task <warning descr="Cannot resolve symbol 'pidt1_'"><weak_warning descr="Cannot infer argument types">pidt1_</weak_warning></warning>
tasks.task <warning descr="Cannot resolve symbol 'tidt1_'"><weak_warning descr="Cannot infer argument types">tidt1_</weak_warning></warning>
''', GrUnresolvedAccessInspection, GroovyAssignabilityCheckInspection, GroovyAccessibilityInspection
  }
}
