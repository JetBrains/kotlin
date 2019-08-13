// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.packaging.impl.elements.DirectoryPackagingElement;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class DirectoryElementPresentation extends PackagingElementPresentation {
  private final DirectoryPackagingElement myElement;

  public DirectoryElementPresentation(DirectoryPackagingElement element) {
    myElement = element;
  }

  @Override
  public String getPresentableName() {
    return myElement.getDirectoryName();
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    presentationData.setIcon(PlatformIcons.FOLDER_ICON);
    presentationData.addText(myElement.getDirectoryName(), mainAttributes);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.DIRECTORY;
  }
}
