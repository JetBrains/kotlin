/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.testFramework.JavaModuleTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import com.intellij.util.Chunk;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class ModuleCompilerUtilTest extends JavaModuleTestCase {
  private TempDirTestFixture myTempDirTestFixture;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myTempDirTestFixture = new TempDirTestFixtureImpl();
    myTempDirTestFixture.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      myTempDirTestFixture.tearDown();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testNoCyclicDependencies() throws IOException {
    Module a = createModule("a");
    Module b = createModule("b");
    PsiTestUtil.addSourceRoot(a, myTempDirTestFixture.findOrCreateDir("a-main"));
    PsiTestUtil.addSourceRoot(a, myTempDirTestFixture.findOrCreateDir("a-tests"), true);
    PsiTestUtil.addSourceRoot(b, myTempDirTestFixture.findOrCreateDir("b-main"));
    PsiTestUtil.addSourceRoot(b, myTempDirTestFixture.findOrCreateDir("b-tests"), true);
    ModuleRootModificationUtil.addDependency(a, b);
    assertEmpty(ModuleCompilerUtil.getCyclicDependencies(myProject, Arrays.asList(a, b)));
  }

  public void testDoNotReportTestsCyclesIncludedIntoProductionCycles() throws IOException {
    Module a = createModule("a");
    Module b = createModule("b");
    PsiTestUtil.addSourceRoot(a, myTempDirTestFixture.findOrCreateDir("a-main"));
    PsiTestUtil.addSourceRoot(a, myTempDirTestFixture.findOrCreateDir("a-tests"), true);
    PsiTestUtil.addSourceRoot(b, myTempDirTestFixture.findOrCreateDir("b-main"));
    PsiTestUtil.addSourceRoot(b, myTempDirTestFixture.findOrCreateDir("b-tests"), true);
    ModuleRootModificationUtil.addDependency(a, b);
    ModuleRootModificationUtil.addDependency(b, a);
    List<Chunk<ModuleSourceSet>> cycles = ModuleCompilerUtil.getCyclicDependencies(myProject, Arrays.asList(a, b));
    assertEquals(1, cycles.size());
  }

  public void testIgnoreEmptySourceSets() throws IOException {
    Module a = createModule("a");
    Module b = createModule("b");
    PsiTestUtil.addSourceRoot(a, myTempDirTestFixture.findOrCreateDir("a-main"));
    PsiTestUtil.addSourceRoot(b, myTempDirTestFixture.findOrCreateDir("b-main"));
    PsiTestUtil.addSourceRoot(b, myTempDirTestFixture.findOrCreateDir("b-tests"), true);
    ModuleRootModificationUtil.addDependency(a, b);
    ModuleRootModificationUtil.addDependency(b, a, DependencyScope.TEST, false);
    assertEmpty(ModuleCompilerUtil.getCyclicDependencies(myProject, Arrays.asList(a, b)));
  }
}