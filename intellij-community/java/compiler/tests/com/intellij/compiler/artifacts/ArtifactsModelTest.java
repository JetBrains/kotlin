// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.artifacts;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class ArtifactsModelTest extends ArtifactsTestCase {

  public void testAddArtifact() {
    assertEmpty(getArtifacts());

    final long count = getModificationCount();
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    final ModifiableArtifact artifact = model.addArtifact("new art", PlainArtifactType.getInstance());
    artifact.setBuildOnMake(true);
    artifact.setOutputPath("/myout");
    artifact.setName("xxx");

    final MyArtifactListener listener = subscribe();
    assertEmpty(getArtifacts());
    commit(model);
    final Artifact newArt = assertOneElement(getArtifacts());
    assertEquals("xxx", newArt.getName());
    assertTrue(newArt.isBuildOnMake());
    assertEquals("/myout", newArt.getOutputPath());
    assertEquals("added:xxx;", listener.clearMessages());
    assertTrue(getModificationCount() > count);
  }

  private long getModificationCount() {
    return getArtifactManager().getModificationTracker().getModificationCount();
  }

  public void testRemoveArtifact() {
    Artifact artifact = addArtifact("aaa");
    assertSame(artifact, assertOneElement(getArtifacts()));

    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.removeArtifact(artifact);
    final MyArtifactListener listener = subscribe();
    commit(model);

    assertEmpty(getArtifacts());
    assertEquals("removed:aaa;", listener.clearMessages());
  }

  public void testChangeAndRemoveArtifact() {
    doTestChangeAndRemove(false);
  }

  public void testChangeAndRemoveOriginalArtifact() {
    doTestChangeAndRemove(true);
  }

  private void doTestChangeAndRemove(final boolean removeOriginal) {
    final Artifact artifact = addArtifact("aaa");
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    final ModifiableArtifact modifiable = model.getOrCreateModifiableArtifact(artifact);
    modifiable.setName("bbb");
    model.removeArtifact(removeOriginal ? artifact : modifiable);


    final MyArtifactListener listener = subscribe();
    commit(model);
    assertEmpty(getArtifacts());
    assertEquals("removed:aaa;", listener.clearMessages());
  }

  private static void commit(ModifiableArtifactModel model) {
    ApplicationManager.getApplication().runWriteAction(() -> model.commit());
  }

  public void testChangeArtifact() {
    final Artifact artifact = addArtifact("xxx");

    final long count = getModificationCount();
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    assertFalse(model.isModified());
    assertSame(artifact, model.getArtifactByOriginal(artifact));

    final ModifiableArtifact modifiable = model.getOrCreateModifiableArtifact(artifact);
    assertNotNull(modifiable);
    assertEquals("xxx", modifiable.getName());
    assertNotSame(modifiable, artifact);

    modifiable.setOutputPath("/aaa");
    modifiable.setName("qqq");
    assertEquals(getProject().getBasePath() + "/out/artifacts/xxx", artifact.getOutputPath());
    assertEquals("xxx", artifact.getName());

    assertSame(modifiable, model.getOrCreateModifiableArtifact(artifact));
    assertSame(modifiable, model.getOrCreateModifiableArtifact(modifiable));
    assertSame(modifiable, assertOneElement(model.getArtifacts()));
    assertSame(modifiable, model.findArtifact("qqq"));
    assertNull(model.findArtifact("xxx"));
    assertSame(modifiable, model.getArtifactByOriginal(artifact));
    assertTrue(model.isModified());

    final MyArtifactListener listener = subscribe();
    commit(model);
    assertEquals("changed:xxx->qqq;", listener.clearMessages());
    final Artifact newArtifact = assertOneElement(getArtifacts());
    assertEquals("qqq", newArtifact.getName());
    assertEquals("/aaa", newArtifact.getOutputPath());
    assertSame(newArtifact, artifact);
    assertTrue(getModificationCount() > count);
  }


  private Artifact[] getArtifacts() {
    return getArtifactManager().getArtifacts();
  }

  private MyArtifactListener subscribe() {
    final MyArtifactListener listener = new MyArtifactListener();
    myProject.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, listener);
    return listener;
  }

  private static class MyArtifactListener extends ArtifactAdapter {
    private final StringBuilder myMessages = new StringBuilder();

    @Override
    public void artifactAdded(@NotNull Artifact artifact) {
      myMessages.append("added:").append(artifact.getName()).append(";");
    }

    @Override
    public void artifactRemoved(@NotNull Artifact artifact) {
      myMessages.append("removed:").append(artifact.getName()).append(";");
    }

    @Override
    public void artifactChanged(@NotNull Artifact artifact, @NotNull String oldName) {
      myMessages.append("changed:").append(oldName).append("->").append(artifact.getName()).append(";");
    }

    public String clearMessages() {
      final String messages = myMessages.toString();
      myMessages.setLength(0);
      return messages;
    }
  }
}
