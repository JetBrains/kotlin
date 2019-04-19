package com.intellij.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;

import java.io.File;
import java.io.IOException;

import static com.intellij.util.io.TestFileSystemBuilder.fs;

/**
 * @author nik
 */
public class RecompileOnConfigurationChangeTest extends BaseCompilerTestCase {

  public void testChangeOutput() throws IOException {
    VirtualFile srcRoot = createFile("src/A.java", "class A{}").getParent();
    Module m = addModule("m", srcRoot);
    make(m);
    assertOutput(m, fs().file("A.class"));

    File oldOutput = getOutputDir(m);
    File newOutput = createTempDir("new-output");
    PsiTestUtil.setCompilerOutputPath(m, VfsUtil.pathToUrl(FileUtil.toSystemIndependentName(newOutput.getAbsolutePath())), false);
    make(m);
    assertOutput(m, fs().file("A.class"));
    File[] files = oldOutput.listFiles();
    assertTrue(files == null || files.length == 0);
  }
}
