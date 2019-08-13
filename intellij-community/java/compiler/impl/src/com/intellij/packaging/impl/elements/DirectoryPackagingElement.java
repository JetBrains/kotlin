// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.compiler.ant.Generator;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.AntCopyInstructionCreator;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.ui.DirectoryElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 *
 * classpath is used for exploded WAR and EJB directories under exploded EAR
 */
public class DirectoryPackagingElement extends CompositeElementWithManifest<DirectoryPackagingElement> {
  @NonNls public static final String NAME_ATTRIBUTE = "name";
  private String myDirectoryName;

  public DirectoryPackagingElement() {
    super(PackagingElementFactoryImpl.DIRECTORY_ELEMENT_TYPE);
  }

  public DirectoryPackagingElement(String directoryName) {
    super(PackagingElementFactoryImpl.DIRECTORY_ELEMENT_TYPE);
    myDirectoryName = directoryName;
  }

  @Override
  @NotNull
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DirectoryElementPresentation(this);
  }

  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {

    final List<Generator> children = new ArrayList<>();
    final Generator command = creator.createSubFolderCommand(myDirectoryName);
    if (command != null) {
      children.add(command);
    }
    children.addAll(computeChildrenGenerators(resolvingContext, creator.subFolder(myDirectoryName), generationContext, artifactType));
    return children;
  }

  @Override
  public DirectoryPackagingElement getState() {
    return this;
  }

  @NonNls @Override
  public String toString() {
    return "dir:" + myDirectoryName;
  }

  @Attribute(NAME_ATTRIBUTE)
  public String getDirectoryName() {
    return myDirectoryName;
  }

  public void setDirectoryName(String directoryName) {
    myDirectoryName = directoryName;
  }

  @Override
  public void rename(@NotNull String newName) {
    myDirectoryName = newName;
  }

  @Override
  public String getName() {
    return myDirectoryName;
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof DirectoryPackagingElement && ((DirectoryPackagingElement)element).getDirectoryName().equals(myDirectoryName);
  }

  @Override
  public void loadState(@NotNull DirectoryPackagingElement state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
