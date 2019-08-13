// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.project;

import com.intellij.openapi.externalSystem.service.project.ModifiableModel;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

public interface PackagingModifiableModel extends ModifiableModel {
  @NotNull
  ModifiableArtifactModel getModifiableArtifactModel();

  @NotNull
  PackagingElementResolvingContext getPackagingElementResolvingContext();

  ArtifactExternalDependenciesImporter getArtifactExternalDependenciesImporter();
}
