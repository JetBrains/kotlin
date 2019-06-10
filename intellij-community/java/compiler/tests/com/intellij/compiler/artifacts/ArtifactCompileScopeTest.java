// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.artifacts;

import com.intellij.compiler.CompilerTestUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.util.TimeoutUtil;

import java.io.File;

import static com.intellij.compiler.artifacts.ArtifactsTestCase.commitModel;

/**
 * @author nik
 */
public class ArtifactCompileScopeTest extends ArtifactCompilerTestCase {

  public void testDoNotCleanArtifactOutputOnRebuild()  {
    Artifact a = addArtifact(root().file(createFile("a.txt")));
    make(a);
    createFileInOutput(a, "b.txt");
    assertOutput(a, fs().file("a.txt").file("b.txt"));

    rebuild();
    assertOutput(a, fs().file("a.txt").file("b.txt"));
  }

  public void testMakeArtifactAfterRebuild()  {
    Module m = addModule("m", createFile("src/A.java", "class A{}").getParent());
    Artifact a = addArtifact(root().module(m));
    make(a);
    assertOutput(m, fs().file("A.class"));

    createFileInOutput(a, "a.txt");
    assertOutput(a, fs().file("A.class").file("a.txt"));

    createFile("src/B.java", "class B{}");

    rebuild();
    assertOutput(m, fs().file("A.class").file("B.class"));
    assertOutput(a, fs().file("A.class").file("a.txt"));

    make(a);
    assertOutput(a, fs().file("A.class").file("B.class").file("a.txt"));
  }

  public void testRebuildArtifactOnProjectRebuildIfBuildOnMakeOptionIsEnabled() {
    Module m = addModule("m", createFile("src/A.java", "class A{}").getParent());
    Artifact a = addArtifact(root().module(m));
    setBuildOnMake(a);
    make(a);
    createFileInOutput(a, "a.txt");
    assertOutput(a, fs().file("A.class").file("a.txt"));

    rebuild();
    assertOutput(a, fs().file("A.class").file("a.txt"));
  }

  public void testDoNotRebuildIncludedModulesOnRebuildingArtifact() {
    Module m = addModule("m", createFile("src/AB.java", "class A{} class B{}").getParent());
    Artifact a = addArtifact(root().module(m));
    make(a);
    deleteFileInOutput(a, "A.class");
    deleteFileInOutput(a, "B.class");
    deleteFileInOutput(m, "A.class");
    assertOutput(a, fs());
    assertOutput(m, fs().file("B.class"));

    recompile(a);
    assertOutput(a, fs().file("B.class"));
    assertOutput(m, fs().file("B.class"));
  }

  private static void deleteFileInOutput(Artifact a, final String fileName) {
    boolean deleted = FileUtil.delete(new File(VfsUtilCore.virtualToIoFile(getOutputDir(a)), fileName));
    assertTrue(deleted);
  }

  private static void deleteFileInOutput(Module m, final String fileName) {
    boolean deleted = FileUtil.delete(new File(getOutputDir(m), fileName));
    assertTrue(deleted);
  }

  @Override
  protected void setUpProject() throws Exception {
    super.setUpProject();
    CompilerTestUtil.setupJavacForTests(myProject);
  }

  public void testMakeModule() {
    final VirtualFile file1 = createFile("src1/A.java", "public class A {}");
    final Module module1 = addModule("module1", file1.getParent());
    final VirtualFile file2 = createFile("src2/B.java", "public class B {}");
    final Module module2 = addModule("module2", file2.getParent());

    final Artifact artifact = addArtifact(root().module(module1));

    make(module1);
    assertNoOutput(artifact);

    setBuildOnMake(artifact);

    make(module2);
    assertNoOutput(artifact);

    make(module1);
    assertOutput(artifact, fs().file("A.class"));
  }

  protected void setBuildOnMake(Artifact artifact) {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.getOrCreateModifiableArtifact(artifact).setBuildOnMake(true);
    commitModel(model);
  }

  public void testMakeFullArtifactIfIncludedModuleIsCompiled() {
    final VirtualFile file1 = createFile("src1/A.java", "public class A{}");
    final Module module1 = addModule("module1", file1.getParent());
    final VirtualFile file2 = createFile("src2/B.java", "public class B{}");
    final Module module2 = addModule("module2", file2.getParent());

    final Artifact artifact = addArtifact(root().module(module1).module(module2));

    setBuildOnMake(artifact);

    make(module1);
    assertOutput(artifact, fs().file("A.class").file("B.class"));
  }

  //IDEA-58529
  public void testDoNotRebuildArtifactOnForceCompileOfSingleFile() {
    final VirtualFile file1 = createFile("src/A.java", "public class A{}");
    final VirtualFile file2 = createFile("src/B.java", "public class B{}");
    final Module module = addModule("module", file1.getParent());

    final Artifact artifact = addArtifact(root().module(module));
    setBuildOnMake(artifact);

    make(module);
    assertOutput(artifact, fs().file("A.class").file("B.class"));

    compile(false, file1).assertUpToDate();

    ensureTimeChanged();
    final String[] aPath = {"A.class"};
    //file should be deleted and recompiled by javac and recompiled by artifacts compiler
    compile(true, file1).assertGenerated(aPath);

    ensureTimeChanged();
    make(module);

    ensureTimeChanged();
    final String[] bothPaths = {"A.class", "B.class"};
    compile(true, file1, file2).assertGenerated(bothPaths);
  }

  private static void ensureTimeChanged() {
    //ensure that compiler will threat the file as changed. On Linux system timestamp may be rounded to multiple of 1000
    if (SystemInfo.isLinux) {
      TimeoutUtil.sleep(1100);
    }
  }
}
