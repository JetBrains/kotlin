// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class ModuleOutputPackagingElementBase extends ModulePackagingElementBase implements ModuleOutputPackagingElement {
  public ModuleOutputPackagingElementBase(PackagingElementType type,
                                          Project project,
                                          ModulePointer modulePointer) {
    super(type, project, modulePointer);
  }

  @Nullable
  protected abstract String getDirectoryAntProperty(ArtifactAntGenerationContext generationContext);

  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext,
                                                          @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {
    String property = getDirectoryAntProperty(generationContext);
    if (myModulePointer != null && property != null) {
      final String moduleOutput = BuildProperties.propertyRef(property);
      return Collections.singletonList(creator.createDirectoryContentCopyInstruction(moduleOutput));
    }
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
    return PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES;
  }

  public ModuleOutputPackagingElementBase(PackagingElementType type, Project project) {
    super(type, project);
  }
}
