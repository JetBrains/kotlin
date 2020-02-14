// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.junit.Test
import org.junit.runners.Parameterized

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASKS_JAVADOC_JAVADOC

@CompileStatic
class GradleActionTest extends GradleHighlightingBaseTest {

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  static Collection<Object[]> data() {
    return [[BASE_GRADLE_VERSION].toArray()]
  }


  @Override
  protected List<String> getParentCalls() {
    return []
  }

  @Test
  void test() {
    importProject("apply plugin:'java'")
    new RunAll().append {
      'domain collection forEach'()
    }.append {
      'nested version block'
    } run()
  }

  void 'domain collection forEach'() {
    doTest('tasks.withType(Javadoc).configureEach {\n' +
           '  <caret>\n' +
           '}') {
      closureDelegateTest(GRADLE_API_TASKS_JAVADOC_JAVADOC, 1)
    }
  }


  void 'nested version block'() {
    doTest('dependencies {' +
           ' implementation("group:artifact") {\n' +
           '  version { <caret> }\n' +
           '}') {
      closureDelegateTest(GRADLE_API_TASKS_JAVADOC_JAVADOC, 1)
    }
  }
}
