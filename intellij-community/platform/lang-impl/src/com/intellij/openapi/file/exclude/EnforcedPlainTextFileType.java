// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class EnforcedPlainTextFileType implements FileType {
  public static final FileType INSTANCE = new EnforcedPlainTextFileType();

  private static final Icon ICON = IconLoader
    .createLazy(() -> new LayeredIcon(AllIcons.FileTypes.Text, PlatformIcons.EXCLUDED_FROM_COMPILE_ICON));


  private EnforcedPlainTextFileType() {
    EnforcedPlainTextFileTypeManager.getInstance();
  }

  @NotNull
  @Override
  public String getName() {
    return "Enforced Plain Text";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Enforced Plain Text";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "fakeTxt";
  }

  @Override
  public Icon getIcon() {
    return ICON;
  }

  @Override
  public boolean isBinary() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
    return null;
  }
}
