// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.file.exclude;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class EnforcedPlainTextFileTypeOverrider implements FileTypeOverrider {
  @Nullable
  @Override
  public FileType getOverriddenFileType(@NotNull VirtualFile file) {
    if (EnforcedPlainTextFileTypeManager.getInstance().isMarkedAsPlainText(file)) {
      return EnforcedPlainTextFileType.INSTANCE;
    }
    return null;
  }
}
