// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.impl.ui.ExtractedDirectoryPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExtractedDirectoryPackagingElement extends FileOrDirectoryCopyPackagingElement<ExtractedDirectoryPackagingElement> {
  private String myPathInJar;

  public ExtractedDirectoryPackagingElement() {
    super(PackagingElementFactoryImpl.EXTRACTED_DIRECTORY_ELEMENT_TYPE);
  }

  public ExtractedDirectoryPackagingElement(String jarPath, String pathInJar) {
    super(PackagingElementFactoryImpl.EXTRACTED_DIRECTORY_ELEMENT_TYPE, jarPath);
    myPathInJar = pathInJar;
    if (!StringUtil.startsWithChar(myPathInJar, '/')) {
      myPathInJar = "/" + myPathInJar;
    }
    if (!StringUtil.endsWithChar(myPathInJar, '/')) {
      myPathInJar += "/";
    }
  }

  @NotNull
  @Override
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new ExtractedDirectoryPresentation(this);
  }

  @Override
  public String toString() {
    return "extracted:" + myFilePath + "!" + myPathInJar;
  }

  @Override
  public VirtualFile findFile() {
    final VirtualFile jarFile = super.findFile();
    if (jarFile == null) return null;

    final VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(jarFile);
    if ("/".equals(myPathInJar)) return jarRoot;
    return jarRoot != null ? jarRoot.findFileByRelativePath(myPathInJar) : null;
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof ExtractedDirectoryPackagingElement && super.isEqualTo(element)
           && Objects.equals(myPathInJar, ((ExtractedDirectoryPackagingElement)element).getPathInJar());
  }

  @Override
  public ExtractedDirectoryPackagingElement getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull ExtractedDirectoryPackagingElement state) {
    myFilePath = state.getFilePath();
    myPathInJar = state.getPathInJar();
  }

  @Attribute("path-in-jar")
  public String getPathInJar() {
    return myPathInJar;
  }

  public void setPathInJar(String pathInJar) {
    myPathInJar = pathInJar;
  }
}
