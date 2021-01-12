// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.libraries;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.UnknownLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class UnknownLibraryType extends LibraryType<UnknownLibraryKind.UnknownLibraryProperties> {
  public UnknownLibraryType(@NotNull UnknownLibraryKind kind) {
    super(kind);
  }

  @Override
  public @Nullable String getCreateActionName() {
    return null;
  }

  @Override
  public @Nullable NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent,
                                                            @Nullable VirtualFile contextDirectory,
                                                            @NotNull Project project) {
    return null;
  }

  @Override
  public @Nullable LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<UnknownLibraryKind.UnknownLibraryProperties> editorComponent) {
    return null;
  }

  @Override
  public @Nullable Icon getIcon(UnknownLibraryKind.@Nullable UnknownLibraryProperties properties) {
    return AllIcons.Nodes.Unknown;
  }
}
