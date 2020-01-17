// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class GeneratedSourceFileChangeTrackerTest extends CodeInsightFixtureTestCase {
  private final GeneratedSourcesFilter myGeneratedSourcesFilter = new GeneratedSourcesFilter() {
    @Override
    public boolean isGeneratedSource(@NotNull VirtualFile file, @NotNull Project project) {
      return file.getName().startsWith("Gen");
    }
  };

  @Override
  protected void setUp() throws Exception {
    GeneratedSourceFileChangeTrackerImpl.IN_TRACKER_TEST = true;
    super.setUp();
    ExtensionTestUtil
      .maskExtensions(GeneratedSourcesFilter.EP_NAME, Collections.singletonList(myGeneratedSourcesFilter), getTestRootDisposable());
  }

  @Override
  protected void tearDown() throws Exception {
    GeneratedSourceFileChangeTrackerImpl.IN_TRACKER_TEST = false;
    super.tearDown();
  }

  public void testChangeOrdinary() throws Exception {
    PsiFile file = myFixture.configureByText("Ordinary.txt", "");
    myFixture.type('a');
    assertFalse(isEditedGeneratedFile(file));
  }

  public void testChangeGenerated() throws Exception {
    PsiFile file = myFixture.configureByText("Gen.txt", "");
    myFixture.type('a');
    assertTrue(isEditedGeneratedFile(file));
  }

  public void testChangeGeneratedExternally() throws Exception {
    PsiFile file = myFixture.configureByText("Gen.txt", "");
    myFixture.saveText(file.getVirtualFile(), "abc");
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    assertFalse(isEditedGeneratedFile(file));
  }

  private boolean isEditedGeneratedFile(PsiFile file) throws Exception {
    GeneratedSourceFileChangeTrackerImpl tracker = (GeneratedSourceFileChangeTrackerImpl)getTracker();
    tracker.waitForAlarm(10, TimeUnit.SECONDS);
    return tracker.isEditedGeneratedFile(file.getVirtualFile());
  }

  private GeneratedSourceFileChangeTracker getTracker() {
    return GeneratedSourceFileChangeTracker.getInstance(getProject());
  }
}
