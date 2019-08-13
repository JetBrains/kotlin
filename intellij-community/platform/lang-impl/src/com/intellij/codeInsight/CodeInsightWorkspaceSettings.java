// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * @author peter
 */
@State(name = "CodeInsightWorkspaceSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class CodeInsightWorkspaceSettings implements PersistentStateComponent<CodeInsightWorkspaceSettings> {
  public boolean optimizeImportsOnTheFly = CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY;

  @TestOnly
  public void setOptimizeImportsOnTheFly(boolean optimizeImportsOnTheFly, Disposable parentDisposable) {
    boolean prev = this.optimizeImportsOnTheFly;
    this.optimizeImportsOnTheFly = optimizeImportsOnTheFly;
    Disposer.register(parentDisposable, () -> this.optimizeImportsOnTheFly = prev);
  }

  public static CodeInsightWorkspaceSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, CodeInsightWorkspaceSettings.class);
  }


  @Override
  public void loadState(@NotNull CodeInsightWorkspaceSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Nullable
  @Override
  public CodeInsightWorkspaceSettings getState() {
    return this;
  }
}
