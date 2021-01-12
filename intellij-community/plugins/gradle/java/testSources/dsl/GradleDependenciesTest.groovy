// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.dsl

import com.intellij.psi.PsiMethod
import com.intellij.testFramework.RunAll
import groovy.transform.CompileStatic
import org.jetbrains.plugins.gradle.highlighting.GradleHighlightingBaseTest
import org.jetbrains.plugins.groovy.util.ResolveTest
import org.junit.Test

import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*

@CompileStatic
class GradleDependenciesTest extends GradleHighlightingBaseTest implements ResolveTest {

  @Override
  protected List<String> getParentCalls() {
    return []
  }

  @Test
  void dependenciesTest() {
    importProject("apply plugin: 'java'")
    new RunAll().append {
      'dependencies delegate'()
    } append {
      'add external module dependency delegate'()
    } append {
      'add self resolving dependency delegate'()
    } append {
      'add project dependency delegate'()
    } append {
      'add delegate method setter'()
    } append {
      'module delegate'()
    } append {
      'module delegate method setter'()
    } append {
      'components delegate'()
    } append {
      'modules delegate'()
    } append {
      'modules module delegate'()
    } append {
      'classpath configuration'()
    } append {
      'compile configuration'()
    } append {
      'buildscript classpath configuration'()
    } append {
      'buildscript compile configuration'()
    } run()
  }

  void 'dependencies delegate'() {
    doTest('dependencies { <caret> }') {
      closureDelegateTest(GRADLE_API_DEPENDENCY_HANDLER, 1)
    }
  }

  void 'add external module dependency delegate'() {
    def data = [
      'dependencies { add("compile", name: 42) { <caret> } }',
      'dependencies { add("compile", [name:42]) { <caret> } }',
      'dependencies { add("compile", ":42") { <caret> } }',
      'dependencies { compile(name: 42) { <caret> } }',
      'dependencies { compile([name:42]) { <caret> } }',
      'dependencies { compile(":42") { <caret> } }',
      'dependencies.add("compile", name: 42) { <caret> }',
      'dependencies.add("compile", [name:42]) { <caret> }',
      'dependencies.add("compile", ":42") { <caret> }',
      'dependencies.compile(name: 42) { <caret> }',
      'dependencies.compile([name:42]) { <caret> }',
      'dependencies.compile(":42") { <caret> }',
    ]
    doTest(data) {
      closureDelegateTest(GRADLE_API_ARTIFACTS_EXTERNAL_MODULE_DEPENDENCY, 1)
    }
  }

  void 'add self resolving dependency delegate'() {
    def data = [
      'dependencies { add("compile", files()) { <caret> } }',
      'dependencies { add("compile", fileTree("libs")) { <caret> } }',
      'dependencies { compile(files()) { <caret> } }',
      'dependencies { compile(fileTree("libs")) { <caret> } }',
      'dependencies.add("compile", files()) { <caret> }',
      'dependencies.add("compile", fileTree("libs")) { <caret> }',
      'dependencies.compile(files()) { <caret> }',
      'dependencies.compile(fileTree("libs")) { <caret> }',
    ]
    doTest(data) {
      closureDelegateTest(GRADLE_API_ARTIFACTS_SELF_RESOLVING_DEPENDENCY, 1)
    }
  }

  void 'add project dependency delegate'() {
    def data = [
      'dependencies { add("compile", project(":")) { <caret> } }',
      'dependencies { compile(project(":")) { <caret> } }',
      'dependencies.add("compile", project(":")) { <caret> }',
      'dependencies.compile(project(":")) { <caret> }',
    ]
    doTest(data) {
      closureDelegateTest(GRADLE_API_ARTIFACTS_PROJECT_DEPENDENCY, 1)
    }
  }

  void 'add delegate method setter'() {
    doTest('dependencies { add("compile", "notation") { <caret>transitive(false) } }') {
      setterMethodTest('transitive', 'setTransitive', GRADLE_API_ARTIFACTS_MODULE_DEPENDENCY)
    }
  }

  void 'module delegate'() {
    doTest('dependencies { module(":") {<caret>} }') {
      closureDelegateTest(GRADLE_API_ARTIFACTS_CLIENT_MODULE_DEPENDENCY, 1)
    }
  }

  void 'module delegate method setter'() {
    doTest('dependencies { module(":") { <caret>changing(true) } }') {
      setterMethodTest('changing', 'setChanging', GRADLE_API_ARTIFACTS_EXTERNAL_MODULE_DEPENDENCY)
    }
  }

  void 'components delegate'() {
    doTest('dependencies { components {<caret>} }') {
      closureDelegateTest(GRADLE_API_COMPONENT_METADATA_HANDLER, 1)
    }
  }

  void 'modules delegate'() {
    doTest('dependencies { modules {<caret>} }') {
      closureDelegateTest(GRADLE_API_COMPONENT_MODULE_METADATA_HANDLER, 1)
    }
  }

  void 'modules module delegate'() {
    doTest('dependencies { modules { module(":") { <caret> } } }') {
      closureDelegateTest(GRADLE_API_COMPONENT_MODULE_METADATA_DETAILS, 1)
    }
  }

  void 'classpath configuration'() {
    doTest('dependencies { <caret>classpath("hi") }') {
      resolveTest(null)
    }
  }

  void 'compile configuration'() {
    doTest('dependencies { <caret>compile("hi") }') {
      methodTest(resolveTest(PsiMethod), "compile", GRADLE_API_DEPENDENCY_HANDLER)
    }
  }

  void 'compile confiduration via property'() {
    doTest('dependencies.<caret>testCompile("hi")') {
      methodTest(resolveTest(PsiMethod), "testCompile", GRADLE_API_DEPENDENCY_HANDLER)
    }
  }

  void 'buildscript classpath configuration'() {
    doTest('buildscript { dependencies { <caret>classpath("hi") } }') {
      methodTest(resolveTest(PsiMethod), "classpath", GRADLE_API_DEPENDENCY_HANDLER)
    }
  }

  void 'buildscript compile configuration'() {
    doTest('buildscript { dependencies { <caret>compile("hi") } }') {
      resolveTest(null)
    }
  }
}
