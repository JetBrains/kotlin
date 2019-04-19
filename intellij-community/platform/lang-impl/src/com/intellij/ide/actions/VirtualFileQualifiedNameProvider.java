// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface VirtualFileQualifiedNameProvider {
  ExtensionPointName<VirtualFileQualifiedNameProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.virtualFileQualifiedNameProvider");

  /**
   * @return {@code virtualFile} fqn (relative path for example) or null if not handled by this provider
   */
  @Nullable
  String getQualifiedName(@NotNull Project project, @NotNull VirtualFile virtualFile);
}