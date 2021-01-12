// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.options.SchemeManager;
import com.intellij.openapi.options.SchemeManagerFactory;
import com.intellij.openapi.options.SchemeProcessor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import gnu.trove.THashSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseToolManager<T extends Tool> {
  private final SchemeManager<ToolsGroup<T>> mySchemeManager;

  public BaseToolManager(@NotNull SchemeManagerFactory factory, @NotNull String schemePath, @NotNull String presentableName) {
    //noinspection AbstractMethodCallInConstructor
    mySchemeManager = factory.create(schemePath, createProcessor(), presentableName);
    mySchemeManager.loadSchemes();
  }

  protected abstract SchemeProcessor<ToolsGroup<T>, ToolsGroup<T>> createProcessor();

  @Nullable
  protected ActionManagerEx getActionManager() {
    return ActionManagerEx.getInstanceEx();
  }

  @Nullable
  public static String convertString(String s) {
    return StringUtil.nullize(s, true);
  }

  public List<T> getTools() {
    List<T> result = new SmartList<>();
    for (ToolsGroup<T> group : mySchemeManager.getAllSchemes()) {
      result.addAll(group.getElements());
    }
    return result;
  }

  @NotNull
  public List<T> getTools(@NotNull String group) {
    ToolsGroup<T> groupByName = mySchemeManager.findSchemeByName(group);
    if (groupByName == null) {
      return Collections.emptyList();
    }
    else {
      return groupByName.getElements();
    }
  }

  public List<ToolsGroup<T>> getGroups() {
    return mySchemeManager.getAllSchemes();
  }

  public void setTools(@NotNull List<ToolsGroup<T>> tools) {
    mySchemeManager.setSchemes(tools);
    registerActions(getActionManager());
  }

  protected final void registerActions(@Nullable ActionManager actionManager) {
    if (actionManager == null) {
      return;
    }

    unregisterActions(actionManager);

    // register
    // to prevent exception if 2 or more targets have the same name
    Set<String> registeredIds = new THashSet<>();
    for (T tool : getTools()) {
      String actionId = tool.getActionId();
      if (registeredIds.add(actionId)) {
        actionManager.registerAction(actionId, createToolAction(tool));
      }
    }
  }

  @NotNull
  protected ToolAction createToolAction(@NotNull T tool) {
    return new ToolAction(tool);
  }

  protected abstract String getActionIdPrefix();

  protected void unregisterActions(@Nullable ActionManager actionManager) {
    if (actionManager == null) {
      return;
    }

    for (String oldId : actionManager.getActionIds(getActionIdPrefix())) {
      actionManager.unregisterAction(oldId);
    }
  }
}
