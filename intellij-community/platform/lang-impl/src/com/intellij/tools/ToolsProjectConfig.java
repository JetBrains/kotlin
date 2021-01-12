// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * @author lene
 */
@State(name = "ToolsProjectConfig", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class ToolsProjectConfig implements PersistentStateComponent<ToolsProjectConfig.State> {
  private String myAfterCommitToolsId;

  public static ToolsProjectConfig getInstance(final Project project) {
    return ServiceManager.getService(project, ToolsProjectConfig.class);
  }

  protected ToolsProjectConfig() {
  }

  public String getAfterCommitToolsId() {
    return myAfterCommitToolsId;
  }

  public void setAfterCommitToolId(String id) {
    myAfterCommitToolsId = id;
  }

  @Override
  public State getState() {
    return new State(myAfterCommitToolsId);
  }

  @Override
  public void loadState(@NotNull State state) {
    myAfterCommitToolsId = state.getAfterCommitToolId();
  }


  @SuppressWarnings("UnusedDeclaration")
  public static class State {
    private String myAfterCommitToolId;

    public State() {
    }

    public State(String afterCommitToolId) {
      myAfterCommitToolId = afterCommitToolId;
    }

    @Tag(value = "afterCommitToolId")
    public String getAfterCommitToolId() {
      return myAfterCommitToolId;
    }

    public void setAfterCommitToolId(String id) {
      myAfterCommitToolId = id;
    }
  }
}
