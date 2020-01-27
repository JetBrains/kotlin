// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a reference to some folder used in the project configuration to be shown in the content roots editor.
 * @see ExternalContentFolderRef
 */
@ApiStatus.Internal
public interface ContentFolderRef {
  @Nullable VirtualFile getFile();

  @NotNull String getUrl();

  @Nullable ContentFolder getContentFolder();
}
