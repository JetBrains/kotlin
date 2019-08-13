package com.intellij.compiler.artifacts;

import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;

/**
 * @author nik
 */
public class ArtifactPointerTest extends ArtifactsTestCase {
  public void testCreateFromName() {
    final Artifact artifact = addArtifact("art");
    final ArtifactPointer pointer = getPointerManager().createPointer("art");
    assertSame(artifact, pointer.getArtifact());
    assertSame(artifact, pointer.findArtifact(getArtifactManager()));
    assertSame(pointer, getPointerManager().createPointer(artifact));
    assertEquals("art", pointer.getArtifactName());
  }

  public void testCreateFromArtifact() {
    final Artifact artifact = addArtifact("aaa");
    final ArtifactPointer pointer = getPointerManager().createPointer(artifact);
    assertSame(artifact, pointer.getArtifact());
    assertEquals("aaa", pointer.getArtifactName());
    assertSame(pointer, getPointerManager().createPointer("aaa"));
  }

  public void testRenameArtifact() {
    final Artifact artifact = addArtifact("art1");
    final ArtifactPointer pointer = getPointerManager().createPointer("art1");
    assertSame(artifact, pointer.getArtifact());
    assertEquals("art1", pointer.getArtifactName());

    final Artifact newArtifact = rename(artifact, "art2");
    assertSame(newArtifact, pointer.getArtifact());
    assertEquals("art2", pointer.getArtifactName());
    assertSame(pointer, getPointerManager().createPointer("art2"));
  }

  public void testCreateArtifactAfterPointer() {
    final ArtifactPointer pointer = getPointerManager().createPointer("xxx");
    assertNull(pointer.getArtifact());

    final Artifact artifact = addArtifact("xxx");
    assertSame(artifact, pointer.getArtifact());
    assertSame(pointer, getPointerManager().createPointer("xxx"));
    assertSame(pointer, getPointerManager().createPointer(artifact));
  }

  public void testDeleteArtifact() {
    final Artifact artifact = addArtifact("abc");
    final ArtifactPointer pointer = getPointerManager().createPointer(artifact);
    assertSame(artifact, pointer.getArtifact());

    deleteArtifact(artifact);
    assertNull(pointer.getArtifact());
  }

  public void testRemoveAndAdd() {
    final Artifact artifact = addArtifact("abc");
    final ArtifactPointer pointer = getPointerManager().createPointer(artifact);
    assertSame(artifact, pointer.getArtifact());

    deleteArtifact(artifact);

    assertNull(pointer.getArtifact());

    final Artifact artifact2 = addArtifact("abc");
    assertSame(artifact2, pointer.getArtifact());
    assertSame(pointer, getPointerManager().createPointer(artifact2));
  }

  public void testReloadArtifact() {
    final Artifact oldArtifact = addArtifact("xxx");
    final ArtifactPointer pointer = getPointerManager().createPointer(oldArtifact);

    ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    model.removeArtifact(oldArtifact);
    final ModifiableArtifact newArtifact = model.addArtifact("xxx", PlainArtifactType.getInstance());
    commitModel(model);

    assertSame(pointer, getPointerManager().createPointer(newArtifact));
    assertSame(pointer, getPointerManager().createPointer("xxx"));
    assertSame(newArtifact, pointer.getArtifact());

    model = getArtifactManager().createModifiableModel();
    model.removeArtifact(newArtifact);
    final ModifiableArtifact newArtifact2 = model.addArtifact("xxx", PlainArtifactType.getInstance());
    commitModel(model);

    assertSame(pointer, getPointerManager().createPointer(newArtifact2));
    assertSame(pointer, getPointerManager().createPointer("xxx"));
    assertSame(newArtifact2, pointer.getArtifact());
  }

  public void testRenameModifiable() {
    final Artifact a = addArtifact("a");
    final ArtifactPointer p = getPointerManager().createPointer(a);

    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    assertSame(a, p.getArtifact());
    final ModifiableArtifact modifiable = model.getOrCreateModifiableArtifact(a);
    assertSame(modifiable, p.findArtifact(model));
    modifiable.setName("b");
    assertSame(a, p.getArtifact());
    assertSame(modifiable, p.findArtifact(model));
    assertEquals("a", p.getArtifactName());
    assertEquals("b", p.getArtifactName(model));

    commitModel(model);
    assertSame(a, p.getArtifact());
    assertEquals("b", p.getArtifactName());
  }

  public void testModifyArtifact() {
    final Artifact artifact = addArtifact("a");
    ArtifactPointer pointer = getPointerManager().createPointer(artifact);

    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    assertSame(artifact, pointer.getArtifact());

    final ModifiableArtifact modifiable = model.getOrCreateModifiableArtifact(artifact);
    assertSame(artifact, pointer.getArtifact());
    assertSame(modifiable, pointer.findArtifact(model));
    assertSame(artifact, pointer.getArtifact());

    assertSame(pointer, getPointerManager().createPointer(artifact));
    assertSame(pointer, getPointerManager().createPointer("a"));

    commitModel(model);

    assertSame(artifact, pointer.getArtifact());
  }

  public void testSurviveAfterCommit() {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    final ModifiableArtifact a = model.addArtifact("a", PlainArtifactType.getInstance());
    final ArtifactPointer pointer = getPointerManager().createPointer(a);
    assertSame(a, pointer.getArtifact());
    assertSame(a, pointer.findArtifact(model));
    model.getOrCreateModifiableArtifact(a).setName("b");
    assertEquals("b", pointer.getArtifactName(model));

    assertSame(pointer, getPointerManager().createPointer(a));

    commitModel(model);

    final Artifact b = getArtifactManager().findArtifact("b");
    assertNotNull(b);
    assertSame(b, pointer.getArtifact());
    assertEquals("b", pointer.getArtifactName());

    rename(b, "c");
    assertSame(b, pointer.getArtifact());
    assertEquals("c", pointer.getArtifactName());
  }
  
  public void testSurviveAfterReload() {
    final Artifact artifact = addArtifact("a");
    final ArtifactPointer pointer = getPointerManager().createPointer(artifact);
    assertSame(artifact, pointer.getArtifact());

    deleteArtifact(artifact);
    assertNull(pointer.getArtifact());

    final Artifact newArtifact = addArtifact("a");
    assertSame(newArtifact, pointer.getArtifact());
  }

  public void testDisposePointer() {
    final ModifiableArtifactModel model = getArtifactManager().createModifiableModel();
    final ModifiableArtifact artifact = model.addArtifact("a", PlainArtifactType.getInstance());
    final ArtifactPointer pointer = getPointerManager().createPointer(artifact, model);
    assertSame(artifact, pointer.getArtifact());
    model.dispose();
    assertNull(pointer.getArtifact());
  }

  private ArtifactPointerManager getPointerManager() {
    return ArtifactPointerManager.getInstance(myProject);
  }
}
