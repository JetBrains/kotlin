// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.elements;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NotNull;

/**
 * Describes an element in artifact's output layout.
 *
 * @see com.intellij.packaging.artifacts.Artifact
 * @see PackagingElementFactory
 */
public abstract class PackagingElement<S> implements PersistentStateComponent<S> {
  private final PackagingElementType myType;

  protected PackagingElement(@NotNull PackagingElementType type) {
    myType = type;
  }

  @NotNull
  public abstract PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context);

  @NotNull
  public final PackagingElementType getType() {
    return myType;
  }

  public abstract boolean isEqualTo(@NotNull PackagingElement<?> element);

  @NotNull
  public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
    return PackagingElementOutputKind.OTHER;
  }
}
