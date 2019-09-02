// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.encoding;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import org.jetbrains.annotations.Nullable;

public interface LargeFileEditorAccessor {

  /**
   * @return null - if no access
   */
  @Nullable
  LargeFileEditorAccess getAccess(@Nullable Project project, @Nullable StatusBar statusBar);
}
