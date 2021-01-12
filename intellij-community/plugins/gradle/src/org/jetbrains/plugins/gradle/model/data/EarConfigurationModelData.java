// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.DependencyData;
import com.intellij.serialization.PropertyMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class EarConfigurationModelData extends AbstractExternalEntityData implements ArtifactConfiguration {
  @NotNull
  public static final Key<EarConfigurationModelData> KEY =
    Key.create(EarConfigurationModelData.class, WebConfigurationModelData.KEY.getProcessingWeight() + 1);

  @NotNull
  private final List<Ear> ears;
  @NotNull
  private final Collection<DependencyData> deployDependencies;
  @NotNull
  private final Collection<DependencyData> earlibDependencies;

  @PropertyMapping({"owner", "ears", "deployDependencies", "earlibDependencies"})
  public EarConfigurationModelData(@NotNull ProjectSystemId owner,
                                   @NotNull List<Ear> ears,
                                   @NotNull Collection<DependencyData> deployDependencies,
                                   @NotNull Collection<DependencyData> earlibDependencies) {
    super(owner);

    this.ears = ears;
    this.deployDependencies = deployDependencies;
    this.earlibDependencies = earlibDependencies;
  }

  @SuppressWarnings("unused")
  private EarConfigurationModelData() {
    super(ProjectSystemId.IDE);

    ears = new ArrayList<>();
    deployDependencies = new ArrayList<>();
    earlibDependencies = new ArrayList<>();
  }

  @Override
  @NotNull
  public List<Ear> getArtifacts() {
    return ears;
  }

  @NotNull
  public Collection<DependencyData> getDeployDependencies() {
    return deployDependencies;
  }

  @NotNull
  public Collection<DependencyData> getEarlibDependencies() {
    return earlibDependencies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EarConfigurationModelData)) return false;
    if (!super.equals(o)) return false;

    EarConfigurationModelData data = (EarConfigurationModelData)o;

    if (!ears.equals(data.ears)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + ears.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ears='" + ears + '\'';
  }
}
