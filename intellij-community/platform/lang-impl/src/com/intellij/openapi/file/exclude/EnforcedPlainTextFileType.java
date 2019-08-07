// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class EnforcedPlainTextFileType implements FileTypeIdentifiableByVirtualFile {
  public static final FileType INSTANCE = new EnforcedPlainTextFileType();

  private final EnforcedPlainTextFileTypeManager myTypeManager = EnforcedPlainTextFileTypeManager.getInstance();

  private EnforcedPlainTextFileType() {
  }

  @Override
  public boolean isMyFileType(@NotNull VirtualFile file) {
    return myTypeManager != null && myTypeManager.isMarkedAsPlainText(file);
  }

  @NotNull
  @Override
  public String getName() {
    return EnforcedPlainTextFileTypeFactory.ENFORCED_PLAIN_TEXT;
  }

  @NotNull
  @Override
  public String getDescription() {
    return EnforcedPlainTextFileTypeFactory.ENFORCED_PLAIN_TEXT;
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "fakeTxt";
  }

  @Override
  public Icon getIcon() {
    return EnforcedPlainTextFileTypeFactory.getEnforcedPlainTextIcon();
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
  public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
    return null;
  }
}
