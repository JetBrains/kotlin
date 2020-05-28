// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.artifacts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

public abstract class ArtifactManager implements ArtifactModel {
  public static final Topic<ArtifactListener> TOPIC = Topic.create("artifacts changes", ArtifactListener.class);
  public static final Comparator<Artifact> ARTIFACT_COMPARATOR = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());

  public static ArtifactManager getInstance(@NotNull Project project) {
    return project.getService(ArtifactManager.class);
  }

  /**
   * Return artifacts sorted by their names (ignoring case)
   */
  public abstract Artifact[] getSortedArtifacts();

  public abstract ModifiableArtifactModel createModifiableModel();

  public abstract PackagingElementResolvingContext getResolvingContext();

  @NotNull
  public abstract Artifact addArtifact(@NonNls @NotNull String name, @NotNull ArtifactType type, @Nullable CompositePackagingElement<?> root);

  public abstract void addElementsToDirectory(@NotNull Artifact artifact, @NotNull String relativePath,
                                              @NotNull Collection<? extends PackagingElement<?>> elements);

  public abstract void addElementsToDirectory(@NotNull Artifact artifact, @NotNull String relativePath,
                                              @NotNull PackagingElement<?> element);

  public abstract ModificationTracker getModificationTracker();
}
