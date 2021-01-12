// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.highlighting

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.JavaCodeInsightTestFixtureImpl
import com.intellij.util.SmartList
import com.intellij.util.lang.CompoundRuntimeException
import groovy.transform.CompileStatic
import org.gradle.util.GradleVersion
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.util.ResolveTest

import java.nio.file.Paths

@CompileStatic
abstract class GradleHighlightingBaseTest extends GradleImportingTestCase implements ResolveTest {

  @NotNull
  JavaCodeInsightTestFixture fixture

  @Override
  protected void setUpFixtures() throws Exception {
    myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).fixture
    fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myTestFixture)
    ((JavaCodeInsightTestFixtureImpl)fixture).virtualFileFilter = null
    fixture.setUp()
  }

  void testHighlighting(@NotNull String text) {
    testHighlighting "build.gradle", text
  }

  void testHighlighting(@NotNull String relativePath, @NotNull String text) {
    VirtualFile file = createProjectSubFile relativePath, text
    EdtTestUtil.runInEdtAndWait {
      fixture.testHighlighting(true, false, true, file)
    }
  }

  @Nullable
  PsiElement testResolve(@NotNull String text, @NotNull String substring) {
    VirtualFile vFile = createProjectSubFile "build.gradle", text
    importProject()
    int offset = text.indexOf(substring) + 1
    return ReadAction.compute {
      def file = fixture.psiManager.findFile(vFile)
      PsiReference reference = file.findReferenceAt(offset)
      assert reference != null
      return reference.resolve()
    }
  }

  void doImportAndTest(@NotNull String text, Closure test) {
    updateProjectFile(text)
    importProject()
    ReadAction.run(test)
  }

  void doTest(@NotNull List<String> data, Closure test) {
    List<Throwable> throwables = new SmartList<>()
    for (entry in data) {
      try {
        doTest(entry, test)
      }
      catch (Throwable e) {
        throwables.add(e)
      }
    }
    if (!throwables.isEmpty()) {
      throw new CompoundRuntimeException(throwables)
    }
  }

  void doTest(@NotNull String text, Closure test) {
    doTest(text, getParentCalls(), test)
  }

  void doTest(@NotNull String text, @NotNull List<String> calls, Closure test) {
    List<String> testPatterns = [text]
    calls.each { testPatterns.add("$it { $text }".toString()) }
    testPatterns.each {
      updateProjectFile(it)
      try {
        ReadAction.run(test)
      }
      catch (AssertionError e) {
        throw new AssertionError(it, e)
      }
    }
  }

  void updateProjectFile(@NotNull String text) {
    WriteAction.runAndWait({
      VirtualFile vFile = VfsUtil.findFile(Paths.get(getProjectPath(), "build.gradle"), false)
      if (vFile == null) {
        vFile = createProjectSubFile 'build.gradle', text
      }
      else {
        setFileContent(vFile, text, false)
      }
      fixture.configureFromExistingVirtualFile(vFile)
    })
  }

  protected List<String> getParentCalls() {
    return [
      'project(":")',
      'allprojects',
      'subprojects',
      'configure(project(":"))'
    ]
  }

  @Override
  void tearDownFixtures() {
    fixture.tearDown()
  }

  protected final boolean isGradleAtLeast(@NotNull String version) {
    GradleVersion.version(gradleVersion) >= GradleVersion.version(version)
  }

  protected void setterMethodTest(String name, String originalName, String containingClass) {
    def result = elementUnderCaret(GrMethodCall).advancedResolve()
    def method = assertInstanceOf(result.element, PsiMethod)
    methodTest(method, name, containingClass)
    def original = assertInstanceOf(method.navigationElement, PsiMethod)
    methodTest(original, originalName, containingClass)
  }
}

