// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.artifacts;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.impl.artifacts.ArtifactBySourceFileFinder;

import java.util.Collection;

public class ArtifactBySourceFileFinderTest extends PackagingElementsTestCase {
  public void testAddRemoveDirectoryCopy() {
    final VirtualFile file = createFile("a/b/c.txt");
    assertEmpty(findArtifacts(file));

    final Artifact a = addArtifact(root().dirCopy(file.getParent()));
    assertSame(a, assertOneElement(findArtifacts(file)));

    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.getOrCreateModifiableArtifact(a).getRootElement().removeAllChildren();
    commitModel(model);

    assertEmpty(findArtifacts(file));
  }

  public void testAddRemoveArtifact() {
    final VirtualFile file = createFile("a/b/c.txt");
    assertEmpty(findArtifacts(file));

    final Artifact a = addArtifact(root().file(file));
    assertSame(a, assertOneElement(findArtifacts(file)));

    deleteArtifact(a);

    assertEmpty(findArtifacts(file));
  }

  public void testFindFileUnderSourceRoot() {
    final VirtualFile file = createFile("src/a.xml");
    final Module module = addModule("mmm", file.getParent());
    final Artifact a = addArtifact(root().module(module));
    assertSame(a, assertOneElement(findArtifacts(file)));
  }

  private Collection<? extends Artifact> findArtifacts(VirtualFile file) {
    return ArtifactBySourceFileFinder.getInstance(myProject).findArtifacts(file);
  }
}
