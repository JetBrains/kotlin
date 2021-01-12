// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WebConfigurationModelData extends AbstractExternalEntityData implements ArtifactConfiguration {
  @NotNull
  public static final Key<WebConfigurationModelData> KEY = Key.create(WebConfigurationModelData.class, ExternalSystemConstants.UNORDERED);

  @NotNull
  private final List<War> artifacts;

  @PropertyMapping({"owner", "artifacts"})
  public WebConfigurationModelData(@NotNull ProjectSystemId owner, @NotNull List<War> artifacts) {
    super(owner);
    this.artifacts = artifacts;
  }

  @Override
  @NotNull
  public List<War> getArtifacts() {
    return artifacts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WebConfigurationModelData)) return false;
    if (!super.equals(o)) return false;

    WebConfigurationModelData data = (WebConfigurationModelData)o;

    if (!artifacts.equals(data.artifacts)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + artifacts.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "WebConfigurationModelData{" +
           "myWars=" + artifacts +
           '}';
  }
}
