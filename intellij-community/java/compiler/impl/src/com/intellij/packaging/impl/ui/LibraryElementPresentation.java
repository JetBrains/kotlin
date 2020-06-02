/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.packaging.impl.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.impl.ModuleLibraryTableBase;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablePresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LibraryElementPresentation extends PackagingElementPresentation {
  private final String myLevel;
  private final String myModuleName;
  private final Library myLibrary;
  private final String myLibraryName;
  private final ArtifactEditorContext myContext;

  public LibraryElementPresentation(String libraryName, String level, @Nullable String moduleName, Library library, ArtifactEditorContext context) {
    myLevel = level;
    myModuleName = moduleName;
    myLibrary = library;
    myLibraryName = libraryName;
    myContext = context;
  }

  @Override
  public String getPresentableName() {
    return myLibraryName;
  }

  @Override
  public boolean canNavigateToSource() {
    return myLibrary != null;
  }

  @Override
  public void navigateToSource() {
    myContext.selectLibrary(myLibrary);
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    if (myLibrary != null) {
      presentationData.setIcon(PlatformIcons.LIBRARY_ICON);
      presentationData.addText(myLibraryName, mainAttributes);
      presentationData.addText(getLibraryTableComment(myLibrary), commentAttributes);
    }
    else {
      presentationData.addText(myLibraryName + " (" + (myModuleName != null ? "module '" + myModuleName + "'" : myLevel) + ")",
                               SimpleTextAttributes.ERROR_ATTRIBUTES);
    }
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.LIBRARY;
  }

  public static String getLibraryTableDisplayName(final Library library) {
    LibraryTable table = library.getTable();
    LibraryTablePresentation presentation = table != null ? table.getPresentation() : ModuleLibraryTableBase.MODULE_LIBRARY_TABLE_PRESENTATION;
    return presentation.getDisplayName(false);
  }

  public static String getLibraryTableComment(final Library library) {
    LibraryTable libraryTable = library.getTable();
    String displayName;
    if (libraryTable != null) {
      displayName = libraryTable.getPresentation().getDisplayName(false);
    }
    else {
      Module module = ((LibraryEx)library).getModule();
      String tableName = getLibraryTableDisplayName(library);
      displayName = module != null ? "'" + module.getName() + "' " + tableName : tableName;
    }
    return " (" + displayName + ")";
  }
}
