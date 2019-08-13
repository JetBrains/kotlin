// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.ui.DirectoryCopyPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class DirectoryCopyPackagingElement extends FileOrDirectoryCopyPackagingElement<DirectoryCopyPackagingElement> {
  public DirectoryCopyPackagingElement() {
    super(PackagingElementFactoryImpl.DIRECTORY_COPY_ELEMENT_TYPE);
  }

  public DirectoryCopyPackagingElement(String directoryPath) {
    this();
    myFilePath = directoryPath;
  }

  @NotNull
  @Override
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DirectoryCopyPresentation(myFilePath);
  }

  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext,
                                                          @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {
    final String path = generationContext.getSubstitutedPath(myFilePath);
    return Collections.singletonList(creator.createDirectoryContentCopyInstruction(path));
  }

  @Override
  public DirectoryCopyPackagingElement getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull DirectoryCopyPackagingElement state) {
    myFilePath = state.getFilePath();
  }

  @Override
  public String toString() {
    return "dir:" + myFilePath;
  }
}
