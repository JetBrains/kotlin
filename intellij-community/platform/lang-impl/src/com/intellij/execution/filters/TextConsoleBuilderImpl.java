/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.execution.filters;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author dyoma
 */
public class TextConsoleBuilderImpl extends TextConsoleBuilder {
  private final Project myProject;
  private final GlobalSearchScope myScope;
  private final List<Filter> myFilters = new SmartList<>();
  private boolean myViewer;
  private boolean myUsePredefinedMessageFilter = true;

  public TextConsoleBuilderImpl(@NotNull Project project) {
    this(project, GlobalSearchScope.allScope(project));
  }

  public TextConsoleBuilderImpl(@NotNull Project project, @NotNull GlobalSearchScope scope) {
    myProject = project;
    myScope = scope;
  }

  @NotNull
  @Override
  public ConsoleView getConsole() {
    final ConsoleView consoleView = createConsole();
    for (final Filter filter : myFilters) {
      consoleView.addMessageFilter(filter);
    }
    return consoleView;
  }

  @NotNull
  protected ConsoleView createConsole() {
    return new ConsoleViewImpl(myProject, myScope, myViewer, myUsePredefinedMessageFilter);
  }

  @Override
  public void addFilter(@NotNull Filter filter) {
    myFilters.add(filter);
  }

  @NotNull
  @Override
  public TextConsoleBuilder filters(@NotNull List<? extends Filter> filters) {
    myFilters.addAll(filters);
    return this;
  }

  @Override
  public void setViewer(boolean isViewer) {
    myViewer = isViewer;
  }

  @NotNull
  protected Project getProject() {
    return myProject;
  }

  protected GlobalSearchScope getScope() {
    return myScope;
  }

  @NotNull
  protected List<Filter> getFilters() {
    return myFilters;
  }

  protected boolean isViewer() {
    return myViewer;
  }

  public void setUsePredefinedMessageFilter(boolean usePredefinedMessageFilter) {
    myUsePredefinedMessageFilter = usePredefinedMessageFilter;
  }

  public boolean isUsePredefinedMessageFilter() {
    return myUsePredefinedMessageFilter;
  }
}