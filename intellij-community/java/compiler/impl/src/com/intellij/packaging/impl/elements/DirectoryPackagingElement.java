// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.ui.DirectoryElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
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
