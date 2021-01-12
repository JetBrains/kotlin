/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.slicer;

import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

public class GroupByLeavesAction extends AnAction {
  private final SliceTreeBuilder myTreeBuilder;

  public GroupByLeavesAction(@NotNull SliceTreeBuilder treeBuilder) {
    super(LangBundle.messagePointer("action.GroupByLeavesAction.show.original.expression.values.text"),
          LangBundle.messagePointer("action.GroupByLeavesAction.show.original.expression.values.description"), PlatformIcons.XML_TAG_ICON);
    myTreeBuilder = treeBuilder;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setText(LangBundle.message("action.GroupByLeavesAction.show.original.expression.values.text") +
                                (myTreeBuilder.analysisInProgress
                                 ? " " + LangBundle.message("action.GroupByLeavesAction.analysis.in.progress.text") : ""));
    e.getPresentation().setEnabled(isAvailable());
  }

  private boolean isAvailable() {
    return !myTreeBuilder.analysisInProgress && !myTreeBuilder.splitByLeafExpressions;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    myTreeBuilder.switchToGroupedByLeavesNodes();
  }
}
