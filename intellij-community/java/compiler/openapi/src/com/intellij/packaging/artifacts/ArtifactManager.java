/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * @author nik
 */
public abstract class ArtifactManager implements ArtifactModel {
  public static final Topic<ArtifactListener> TOPIC = Topic.create("artifacts changes", ArtifactListener.class);
  public static final Comparator<Artifact> ARTIFACT_COMPARATOR = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());

  public static ArtifactManager getInstance(@NotNull Project project) {
    return project.getComponent(ArtifactManager.class);
  }

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
