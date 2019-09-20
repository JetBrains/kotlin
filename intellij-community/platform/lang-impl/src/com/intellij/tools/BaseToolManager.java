/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.options.SchemeManager;
import com.intellij.openapi.options.SchemeManagerFactory;
import com.intellij.openapi.options.SchemeProcessor;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class BaseToolManager<T extends Tool> {
  @Nullable private final ActionManagerEx myActionManager;
  private final SchemeManager<ToolsGroup<T>> mySchemeManager;

  public BaseToolManager(@Nullable ActionManagerEx ex,
                         @NotNull SchemeManagerFactory factory,
                         @NotNull String schemePath,
                         @NotNull String presentableName) {
    myActionManager = ex;
    //noinspection AbstractMethodCallInConstructor
    mySchemeManager = factory.create(schemePath, createProcessor(), presentableName);
    mySchemeManager.loadSchemes();
    registerActions();
  }

  protected abstract SchemeProcessor<ToolsGroup<T>, ToolsGroup<T>> createProcessor();

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

  public String getGroupByActionId(String actionId) {
    for (T tool : getTools()) {
      if (Comparing.equal(actionId, tool.getActionId())) {
        return tool.getGroup();
      }
    }
    return null;
  }

  public List<ToolsGroup<T>> getGroups() {
    return mySchemeManager.getAllSchemes();
  }

  public void setTools(@NotNull List<? extends ToolsGroup<T>> tools) {
    mySchemeManager.setSchemes(tools);
    registerActions();
  }

  void registerActions() {
    if (myActionManager == null) {
      return;
    }

    unregisterActions();

    // register
    // to prevent exception if 2 or more targets have the same name
    Set<String> registeredIds = new THashSet<>();
    for (T tool : getTools()) {
      String actionId = tool.getActionId();
      if (registeredIds.add(actionId)) {
        myActionManager.registerAction(actionId, createToolAction(tool));
      }
    }
  }

  @NotNull
  protected ToolAction createToolAction(@NotNull T tool) {
    return new ToolAction(tool);
  }

  protected abstract String getActionIdPrefix();

  private void unregisterActions() {
    // unregister Tool actions
    if (myActionManager == null) {
      return;
    }
    for (String oldId : myActionManager.getActionIds(getActionIdPrefix())) {
      myActionManager.unregisterAction(oldId);
    }
  }
}
