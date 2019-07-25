// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.artifacts;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;

import java.io.IOException;

/**
 * @author nik
 */
public class UpdateArtifactsAfterRenameTest extends PackagingElementsTestCase {
  public void testRenameFile() {
    final VirtualFile file = createFile("a.txt");
    final Artifact artifact = addArtifact(root().dir("xxx").file(file));
    renameFile(file, "b.txt");
    assertLayout(artifact, "<root>\n" +
                           " xxx/\n" +
                           "  file:" + getProjectBasePath() + "/b.txt");
  }

  public void testRenameDirectory() {
    final VirtualFile dir = createFile("dir/a.txt").getParent();
    final Artifact artifact = addArtifact(root().dirCopy(dir));
    renameFile(dir, "xxx");
    assertLayout(artifact, "<root>\n" +
                           " dir:" + getProjectBasePath() + "/xxx");
  }

  public void testMoveFile() {
    final VirtualFile file = createFile("a/xxx.txt");
    final Artifact artifact = addArtifact(root().file(file));
    VirtualFile baseDir = getOrCreateProjectBaseDir();
    moveFile(file, createChildDirectory(baseDir, "b"));
    assertLayout(artifact, "<root>\n" +
                           " file:" + getProjectBasePath() + "/b/xxx.txt");
  }

  public void testRenameParentDir() {
    final VirtualFile file = createFile("x/a.txt");
    final Artifact artifact = addArtifact(root().file(file));
    renameFile(file.getParent(), "y");
    assertLayout(artifact, "<root>\n" +
                           " file:" + getProjectBasePath() + "/y/a.txt");
  }

  public void testMoveParentDir() {
    final VirtualFile file = createFile("a/b/c.txt");
    final Artifact artifact = addArtifact(root().file(file));
    VirtualFile baseDir = getOrCreateProjectBaseDir();
    moveFile(file.getParent(), createChildDirectory(baseDir, "d"));
    assertLayout(artifact, "<root>\n" +
                           " file:" + getProjectBasePath() + "/d/b/c.txt");
  }

  public void testRenameArtifact() {
    final Artifact xxx = addArtifact("xxx");
    final Artifact artifact = addArtifact(root().artifact(xxx));
    rename(xxx, "yyy");
    assertLayout(artifact, "<root>\n" +
                           " artifact:yyy");
  }

  public void testRenameModule() throws ModuleWithNameAlreadyExists {
    final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    final Module module = WriteAction.computeAndWait(() -> {
      Module res = moduleManager.newModule(getProjectBasePath() + "/myModule.iml", StdModuleTypes.JAVA.getId());
      return res;
    });
    final Artifact artifact = addArtifact(root().module(module).moduleSource(module));

    assertLayout(artifact, "<root>\n" +
                           " module:myModule\n" +
                           " module sources:myModule");
    WriteAction.runAndWait(() -> {
      final ModifiableModuleModel model = moduleManager.getModifiableModel();
      model.renameModule(module, "newName");
      model.commit();
    });

    assertLayout(artifact, "<root>\n" +
                           " module:newName\n" +
                           " module sources:newName");

    moduleManager.disposeModule(module);

    assertLayout(artifact, "<root>\n" +
                           " module:newName\n" +
                           " module sources:newName");
  }

  private void moveFile(final VirtualFile file, final VirtualFile newParent) {
    try {
      WriteAction.runAndWait(() -> file.move(this, newParent));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
