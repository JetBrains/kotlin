// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.artifacts;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author nik
 */
public abstract class ArtifactPropertiesProvider {
  public static final ExtensionPointName<ArtifactPropertiesProvider> EP_NAME = ExtensionPointName.create("com.intellij.packaging.artifactPropertiesProvider");
  private final String myId;

  protected ArtifactPropertiesProvider(@NotNull @NonNls String id) {
    myId = id;
  }

  public final String getId() {
    return myId;
  }

  public boolean isAvailableFor(@NotNull ArtifactType type) {
    return true;
  }

  @NotNull
  public abstract ArtifactProperties<?> createProperties(@NotNull ArtifactType artifactType);

  @NotNull
  public static List<ArtifactPropertiesProvider> getProviders() {
    return EP_NAME.getExtensionList();
  }

  @Nullable
  public static ArtifactPropertiesProvider findById(@NotNull @NonNls String id) {
    for (ArtifactPropertiesProvider provider : getProviders()) {
      if (provider.getId().equals(id)) {
        return provider;
      }
    }
    return null;
  }
}
