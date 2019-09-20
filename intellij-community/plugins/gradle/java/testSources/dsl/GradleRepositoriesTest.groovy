// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*

@CompileStatic
class GradleRepositoriesTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Test
  void repositoriesTest() {
    importProject("")
    new RunAll().append {
      'repositories closure delegate'()
    } append {
      'maven repository closure delegate'()
    } append {
      'ivy repository closure delegate'()
    } append {
      'flat repository closure delegate'()
    } append {
      'maven repository method setter'()
    } append {
      'ivy repository method setter'()
    } append {
      'flat repository method setter'()
    } run()
  }

  @Override
  protected List<String> getParentCalls() {
    return super.getParentCalls() + 'buildscript'
  }

  void 'repositories closure delegate'() {
    doTest('repositories { <caret> }') {
      closureDelegateTest(GRADLE_API_REPOSITORY_HANDLER, 1)
    }
  }

  void 'maven repository closure delegate'() {
    doTest('repositories { maven { <caret> } }') {
      closureDelegateTest(GRADLE_API_ARTIFACTS_REPOSITORIES_MAVEN_ARTIFACT_REPOSITORY, 1)
    }
  }

  void 'ivy repository closure delegate'() {
    doTest('repositories { ivy { <caret> } }') {
      closureDelegateTest(GRADLE_API_ARTIFACTS_REPOSITORIES_IVY_ARTIFACT_REPOSITORY, 1)
    }
  }

  void 'flat repository closure delegate'() {
    doTest('repositories { flatDir { <caret> } }') {
      closureDelegateTest(GRADLE_API_ARTIFACTS_REPOSITORIES_FLAT_DIRECTORY_ARTIFACT_REPOSITORY, 1)
    }
  }

  void 'maven repository method setter'() {
    doTest('repositories { maven { <caret>url(42) } }') {
      setterMethodTest('url', 'setUrl', GRADLE_API_ARTIFACTS_REPOSITORIES_MAVEN_ARTIFACT_REPOSITORY)
    }
  }

  void 'ivy repository method setter'() {
    doTest('repositories { ivy { <caret>url("") } }') {
      setterMethodTest('url', 'setUrl', GRADLE_API_ARTIFACTS_REPOSITORIES_IVY_ARTIFACT_REPOSITORY)
    }
  }

  void 'flat repository method setter'() {
    doTest('repositories { flatDir { <caret>name("") } }') {
      setterMethodTest('name', 'setName', GRADLE_API_ARTIFACTS_REPOSITORIES_ARTIFACT_REPOSITORY)
    }
  }
}
