// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model.data;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.DependencyData;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class EarConfigurationModelData extends AbstractExternalEntityData implements ArtifactConfiguration {
  private static final long serialVersionUID = 1L;

  @NotNull
  public static final Key<EarConfigurationModelData> KEY =
    Key.create(EarConfigurationModelData.class, WebConfigurationModelData.KEY.getProcessingWeight() + 1);

  @NotNull
  private final List<Ear> myEars;
  @NotNull
  private final Collection<DependencyData> myDeployDependencies;
  @NotNull
  private final Collection<DependencyData> myEarlibDependencies;

  public EarConfigurationModelData(@NotNull ProjectSystemId owner,
                                   @NotNull List<Ear> ears,
                                   @NotNull Collection<DependencyData> deployDependencies,
                                   @NotNull Collection<DependencyData> earlibDependencies) {
    super(owner);
    myEars = ears;
    myDeployDependencies = deployDependencies;
    myEarlibDependencies = earlibDependencies;
  }

  @Override
  @NotNull
  public List<Ear> getArtifacts() {
    return myEars;
  }

  @NotNull
  public Collection<DependencyData> getDeployDependencies() {
    return myDeployDependencies;
  }

  @NotNull
  public Collection<DependencyData> getEarlibDependencies() {
    return myEarlibDependencies;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EarConfigurationModelData)) return false;
    if (!super.equals(o)) return false;

    EarConfigurationModelData data = (EarConfigurationModelData)o;

    if (!myEars.equals(data.myEars)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myEars.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ears='" + myEars + '\'';
  }
}
