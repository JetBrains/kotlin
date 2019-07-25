/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.tools;

import com.intellij.execution.process.ProcessListener;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Eugene Belyaev
 */
public class ToolAction extends AnAction implements DumbAware {
  private final String myActionId;

  public ToolAction(@NotNull Tool tool) {
    myActionId = tool.getActionId();
    getTemplatePresentation().setText(tool.getName(), false);
    getTemplatePresentation().setDescription(tool.getDescription());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    runTool(myActionId, e.getDataContext(), e, 0L, null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Tool tool = findTool(myActionId, e.getDataContext());
    if (tool != null) {
      e.getPresentation().setText(ToolRunProfile.expandMacrosInName(tool, e.getDataContext()), false);
    }
  }

  private static Tool findTool(String actionId, DataContext context) {
    MacroManager.getInstance().cacheMacrosPreview(context);
    for (Tool tool : getAllTools()) {
      if (actionId.equals(tool.getActionId())) {
        return tool;
      }
    }
    return null;
  }

  protected static List<Tool> getAllTools() {
    return ToolsProvider.getAllTools();
  }

  static void runTool(String actionId, DataContext context) {
    runTool(actionId, context, null, 0L, null);
  }

  static void runTool(String actionId,
                      DataContext context,
                      @Nullable AnActionEvent e,
                      long executionId,
                      @Nullable ProcessListener processListener) {
    Tool tool = findTool(actionId, context);
    if (tool != null) {
      tool.execute(e, new HackyDataContext(context), executionId, processListener);
    }
  }

  @Nullable
  @Override
  public String getTemplateText() {
    return "External Tool";
  }
}
