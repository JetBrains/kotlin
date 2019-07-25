package com.intellij.compiler;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.io.TestFileSystemBuilder;

import static com.intellij.util.io.TestFileSystemBuilder.fs;

/**
 * @author nik
 */
public class ChangeSourceFileSetTest extends BaseCompilerTestCase {

  public void testAddRemoveSourceRoot() {
    VirtualFile a = createFile("src1/A.java", "class A{}");
    VirtualFile b = createFile("src2/B.java", "class B{}");
    Module module = addModule("a", a.getParent());
    make(module);
    assertModulesUpToDate();
    assertOutput(module, fs().file("A.class"));

    PsiTestUtil.addSourceRoot(module, b.getParent());
    make(module);
    assertOutput(module, fs().file("A.class").file("B.class"));

    PsiTestUtil.removeSourceRoot(module, b.getParent());
    make(module);
    assertOutput(module, fs().file("A.class"));
    assertModulesUpToDate();
  }

  public void testAddRemoveExcludedFolder() {
    VirtualFile a = createFile("src/a/A.java", "package a; class A{}");
    createFile("src/b/B.java", "package b; class B{}");
    Module m = addModule("m", a.getParent().getParent());
    make(m);
    assertModulesUpToDate();
    assertOutput(m, fs().dir("a").file("A.class").end().dir("b").file("B.class"));

    PsiTestUtil.addExcludedRoot(m, a.getParent());
    make(m);
    assertOutput(m, fs().dir("b").file("B.class"));

    PsiTestUtil.removeExcludedRoot(m, a.getParent());
    make(m);
    assertOutput(m, fs().dir("a").file("A.class").end().dir("b").file("B.class"));
  }

  public void testAddRemoveIgnoredPattern() {
    String oldPatterns = FileTypeManager.getInstance().getIgnoredFilesList();
    try {
      VirtualFile src = createFile("src/A.java", "class A{}").getParent();
      createFile("src/Ignored.java", "class Ignored{}");
      createFile("src/IgnoredDir/B.java", "package IgnoredDir; class B{}");
      createFile("src/IgnoredDir/p/C.java", "package IgnoredDir.p; class C{}");
      Module m = addModule("m", src);
      make(m);
      assertModulesUpToDate();
      TestFileSystemBuilder all = fs().file("A.class").file("Ignored.class").dir("IgnoredDir").file("B.class").dir("p").file("C.class");
      assertOutput(m, all);

      setIgnoredFiles(oldPatterns + "Ignored*;");
      make(m);
      assertOutput(m, fs().file("A.class"));

      setIgnoredFiles(oldPatterns);
      make(m);
      assertOutput(m, all);
    }
    finally {
      setIgnoredFiles(oldPatterns);
    }
  }

  public void testChangeResourcePatterns() {
    CompilerConfigurationImpl configuration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
    String[] oldPatterns = configuration.getResourceFilePatterns();
    try {
      configuration.removeResourceFilePatterns();
      configuration.addResourceFilePattern("*.xml");
      VirtualFile src = createFile("src/a.txt").getParent();
      createFile("src/b.xml");
      Module m = addModule("m", src);
      make(m);
      assertModulesUpToDate();
      assertOutput(m, fs().file("b.xml"));

      configuration.addResourceFilePattern("*.txt");
      forceFSRescan();
      make(m);
      assertOutput(m, fs().file("a.txt").file("b.xml"));

      configuration.removeResourceFilePatterns();
      configuration.addResourceFilePattern("*.txt");
      forceFSRescan();
      make(m);
      assertOutput(m, fs().file("a.txt"));
    }
    finally {
      configuration.removeResourceFilePatterns();
      for (String pattern : oldPatterns) {
        configuration.addResourceFilePattern(pattern);
      }
    }
  }

  private static void setIgnoredFiles(final String patterns) {
    WriteAction.runAndWait(() -> FileTypeManager.getInstance().setIgnoredFilesList(patterns));
  }
}
