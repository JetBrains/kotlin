// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.SchemeManagerFactory;
import com.intellij.openapi.options.SchemeProcessor;
import org.jetbrains.annotations.NotNull;

@Service
public final class ToolManager extends BaseToolManager<Tool> {
  public ToolManager() {
    super(SchemeManagerFactory.getInstance(), "tools", ToolsBundle.message("tools.settings"));
  }

  static final class MyActionTuner implements ActionConfigurationCustomizer {
    @Override
    public void customize(@NotNull ActionManager manager) {
      getInstance().registerActions(manager);
    }
  }

  public static ToolManager getInstance() {
    return ServiceManager.getService(ToolManager.class);
  }

  @Override
  protected SchemeProcessor<ToolsGroup<Tool>, ToolsGroup<Tool>> createProcessor() {
    return new ToolsProcessor<Tool>() {
      @Override
      protected ToolsGroup<Tool> createToolsGroup(String groupName) {
        return new ToolsGroup<>(groupName);
      }

      @Override
      protected Tool createTool() {
        return new Tool();
      }
    };
  }

  @Override
  protected String getActionIdPrefix() {
    return Tool.ACTION_ID_PREFIX;
  }
}
