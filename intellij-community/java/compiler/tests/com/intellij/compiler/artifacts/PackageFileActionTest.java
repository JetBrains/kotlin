package com.intellij.compiler.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.ui.actions.PackageFileWorker;

import java.io.IOException;

/**
 * @author nik
 */
public class PackageFileActionTest extends ArtifactCompilerTestCase {
  public void testCopyFile() throws Exception {
    final VirtualFile file1 = createFile("a.txt", "text");
    final Artifact artifact = addArtifact(
      root()
        .file(file1)
        .file(createFile("b.txt", "text"))
    );
    compileAndUpdate(file1, "new text");

    assertOutput(artifact, fs().file("a.txt", "new text").file("b.txt", "text"));
  }

  public void testCopyToSubDir() throws Exception {
    final VirtualFile file = createFile("a.txt", "text");
    final Artifact artifact = addArtifact(root().dir("dir").file(file));
    compileAndUpdate(file, "new text");
    assertOutput(artifact, fs().dir("dir").file("a.txt", "new text"));
  }

  public void testCopyDir() throws Exception {
    final VirtualFile file = createFile("a/b/c.txt", "text");
    final Artifact artifact = addArtifact(root().dir("dir").dirCopy(file.getParent().getParent()));
    compileAndUpdate(file, "new");
    assertOutput(artifact, fs().dir("dir").dir("b").file("c.txt", "new"));
  }

  public void testPackFile() throws Exception {
    final VirtualFile file1 = createFile("a.txt", "text");
    final Artifact artifact = addArtifact(
      root()
        .archive("x.zip")
          .file(file1)
          .file(createFile("b.txt", "text"))
    );

    compileAndUpdate(file1, "xxx");

    assertOutput(artifact, fs().archive("x.zip").file("a.txt", "xxx").file("b.txt", "text"));
  }

  public void testPackDir() throws Exception {
    final VirtualFile file = createFile("x/y/z.txt", "42");
    final Artifact artifact = addArtifact(root().archive("a.jar").dir("dir").dirCopy(file.getParent().getParent()));
    compileAndUpdate(file, "239");
    assertOutput(artifact, fs().archive("a.jar").dir("dir").dir("y").file("z.txt", "239"));
  }

  public void testPackFileInNestedArchive() throws Exception {
    VirtualFile file1 = createFile("a.txt", "123");
    VirtualFile file2 = createFile("b.txt", "text");
    final Artifact artifact = addArtifact(
      root()
        .archive("x.ear")
          .archive("w.war")
            .file(file1)
            .file(file2));

    compileAndUpdate(file1, "456");

    assertOutput(artifact, fs()
      .archive("x.ear")
        .archive("w.war")
          .file("a.txt", "456")
          .file("b.txt", "text"));
  }

  public void testAddNewFile() throws Exception {
    final VirtualFile file = createFile("a.txt", "123");
    final Artifact artifact = addArtifact(root().archive("a.jar").file(file));
    packageFile(file);
    assertOutput(artifact, fs().archive("a.jar").file("a.txt", "123"));

    changeFile(file, "456");
    packageFile(file);
    assertOutput(artifact, fs().archive("a.jar").file("a.txt", "456"));
  }

  public void testAddNewFileInNestedArchive() throws Exception {
    VirtualFile file = createFile("a.txt", "123");
    final Artifact artifact = addArtifact(
      root()
        .dir("a").archive("b.jar")
          .dir("c").archive("d.jar")
            .archive("e.jar").file(file));

    packageFile(file);

    assertOutput(artifact, fs()
        .dir("a").archive("b.jar")
          .dir("c").archive("d.jar")
            .archive("e.jar").file("a.txt", "123"));

    changeFile(file, "456");
    packageFile(file);

    assertOutput(artifact, fs()
        .dir("a").archive("b.jar")
          .dir("c").archive("d.jar")
            .archive("e.jar").file("a.txt", "456"));
  }

  public void testFileInIncludedArtifact() throws Exception {
    final VirtualFile file = createFile("a.txt", "321");
    final Artifact a = addArtifact("a", root().dir("x").file(file));
    final Artifact b = addArtifact(root().dir("y").artifact(a));

    compileAndUpdate(file, "123");

    assertOutput(b, fs().dir("y").dir("x").file("a.txt", "123"));
  }

  public void testDoNotCopyFileToItself() throws IOException {
    VirtualFile file = createFile("dir/a.txt", "123");
    Artifact a = addArtifact(root().dirCopy(file.getParent()));
    ArtifactsTestUtil.setOutput(myProject, a.getName(), file.getParent().getPath());
    assertOutput(a, fs().file("a.txt", "123"));
    changeFile(file, "456");
    packageFile(file);
    assertOutput(a, fs().file("a.txt", "456"));
  }

  public void testFileInIncludedArchiveArtifact() throws Exception {
    final VirtualFile file = createFile("a.txt", "xxx");
    final Artifact a = addArtifact("a", archive("a.jar").file(file));
    final Artifact b = addArtifact(archive("b.jar").artifact(a));

    compileAndUpdate(file, "yyy");

    assertOutput(b, fs().archive("b.jar").archive("a.jar").file("a.txt", "yyy"));
  }

  public void testFileUnderSourceRoot() throws Exception {
    final VirtualFile resource = createFile("src/x/a.xml", "old");
    final Module module = addModule("mod", resource.getParent().getParent());
    final Artifact a = addArtifact(root().dir("y").module(module));

    compileAndUpdate(resource, "new");

    assertOutput(a, fs().dir("y").dir("x").file("a.xml", "new"));
  }

  public void testJavaFileUnderSourceRoot() throws Exception {
    final VirtualFile file = createFile("src/a.java", "public class a {}");
    final Module module = addModule("mmm", file.getParent());
    final Artifact a = addArtifact(root().module(module));

    packageFile(file);

    assertNoOutput(a);
  }

  private void compileAndUpdate(VirtualFile file, final String newText) throws Exception {
    compileProject();
    changeFile(file, newText);
    packageFile(file);
  }

  private void packageFile(VirtualFile file) throws IOException {
    PackageFileWorker.packageFile(file, myProject, ArtifactManager.getInstance(myProject).getArtifacts(), true);
  }

}
