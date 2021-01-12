// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.jps.model.java.JavaResourceRootType;

import java.io.IOException;

import static com.intellij.util.io.TestFileSystemBuilder.fs;

public class RecompileAfterVfsChangesTest extends BaseCompilerTestCase {
  public void testMoveFile() throws IOException {
    VirtualFile file = createFile("res/dir1/a.txt", "hello");
    VirtualFile root = file.getParent().getParent();
    Module m = addModuleWithResourceRoot(root);
    makeProjectForTheFirstTime();
    assertOutput(m, fs().dir("dir1").file("a.txt", "hello"));

    WriteAction.run(() -> {
      VirtualFile dir2 = root.createChildDirectory(this, "dir2");
      file.move(this, dir2);
    });
    make(m);
    assertOutput(m, fs().dir("dir2").file("a.txt", "hello"));
  }

  public void testRenameFile() throws IOException {
    VirtualFile file = createFile("res/a.txt", "hello");
    Module m = addModuleWithResourceRoot(file.getParent());
    makeProjectForTheFirstTime();
    assertOutput(m, fs().file("a.txt", "hello"));


    WriteAction.run(() -> file.rename(this, "b.txt"));
    make(m);
    assertOutput(m, fs().file("b.txt", "hello"));
  }

  private void makeProjectForTheFirstTime() {
    buildAllModules();

    //first compilation creates an output directory which causes 'rootChanged' which drops BuildManager's ProjectData cache and forces rescanning,
    // so we need to perform an additional dummy compilation to check that ProjectData is updated correctly
    buildAllModules().assertUpToDate();
  }

  private Module addModuleWithResourceRoot(VirtualFile root) {
    Module m = addModule("m", null);
    PsiTestUtil.addSourceRoot(m, root, JavaResourceRootType.RESOURCE);
    return m;
  }
}
