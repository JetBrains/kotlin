// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import org.jetbrains.annotations.NotNull;

// supplies FileTemplateManager to all clients called from within "withContext"
// useful when they don't have access to the current project to avoid calling FileTemplateManager.getDefaultInstance() because it's more expensive than the one from the current project
public final class VelocityTemplateContext {
  private static final ThreadLocal<FileTemplateManager> ourTemplateManager = new ThreadLocal<>();
  public static <T, E extends Throwable> T withContext(Project project, @NotNull ThrowableComputable<T, E> computable) throws E {
    try {
      ourTemplateManager.set(project == null ? FileTemplateManager.getDefaultInstance() : FileTemplateManager.getInstance(project));
      return computable.compute();
    }
    finally {
      ourTemplateManager.set(null);
    }
  }

  @NotNull
  public static FileTemplateManager getFromContext() {
    FileTemplateManager manager = ourTemplateManager.get();
    return manager == null ? FileTemplateManager.getDefaultInstance() : manager;
  }
}
