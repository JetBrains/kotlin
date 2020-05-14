// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BuildArtifactsBeforeRunTaskBase<Self extends BuildArtifactsBeforeRunTaskBase<?>>
  extends BeforeRunTask<Self> {

  @NonNls public static final String NAME_ATTRIBUTE = "name";

  private List<ArtifactPointer> myArtifactPointers = new ArrayList<>();
  private final Project myProject;
  private final String myElementName;

  protected BuildArtifactsBeforeRunTaskBase(@NotNull Key<Self> providerId, Project project, String elementName) {
    super(providerId);
    myProject = project;
    myElementName = elementName;
  }

  @Override
  public void readExternal(@NotNull Element element) {
    super.readExternal(element);
    final List<Element> children = element.getChildren(myElementName);
    final ArtifactPointerManager pointerManager = ArtifactPointerManager.getInstance(myProject);
    for (Element child : children) {
      myArtifactPointers.add(pointerManager.createPointer(child.getAttributeValue(NAME_ATTRIBUTE)));
    }
  }

  @Override
  public BeforeRunTask clone() {
    final BuildArtifactsBeforeRunTaskBase task = (BuildArtifactsBeforeRunTaskBase)super.clone();
    task.myArtifactPointers = new ArrayList<>(myArtifactPointers);
    return task;
  }

  @Override
  public void writeExternal(@NotNull Element element) {
    super.writeExternal(element);
    for (ArtifactPointer pointer : myArtifactPointers) {
      element.addContent(new Element(myElementName).setAttribute(NAME_ATTRIBUTE, pointer.getArtifactName()));
    }
  }

  @Override
  public int getItemsCount() {
    return myArtifactPointers.size();
  }

  public List<ArtifactPointer> getArtifactPointers() {
    return Collections.unmodifiableList(myArtifactPointers);
  }

  public void setArtifactPointers(List<? extends ArtifactPointer> artifactPointers) {
    myArtifactPointers = new ArrayList<>(artifactPointers);
  }

  public void addArtifact(Artifact artifact) {
    final ArtifactPointer pointer = ArtifactPointerManager.getInstance(myProject).createPointer(artifact);
    if (!myArtifactPointers.contains(pointer)) {
      myArtifactPointers.add(pointer);
    }
  }

  public void removeArtifact(@NotNull Artifact artifact) {
    removeArtifact(ArtifactPointerManager.getInstance(myProject).createPointer(artifact));
  }

  public void removeArtifact(final @NotNull ArtifactPointer pointer) {
    myArtifactPointers.remove(pointer);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    BuildArtifactsBeforeRunTaskBase that = (BuildArtifactsBeforeRunTaskBase)o;

    if (!myArtifactPointers.equals(that.myArtifactPointers)) return false;
    if (!myProject.equals(that.myProject)) return false;

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myArtifactPointers.hashCode();
    result = 31 * result + myProject.hashCode();
    return result;
  }
}
