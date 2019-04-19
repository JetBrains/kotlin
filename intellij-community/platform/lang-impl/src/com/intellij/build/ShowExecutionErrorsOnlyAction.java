// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public class ShowExecutionErrorsOnlyAction extends ToggleAction implements DumbAware {
  private static final Predicate<ExecutionNode> ERROR_FILTER = node -> node.isRunning() || node.isFailed();
  private final Filterable<ExecutionNode> myFilterable;
  private final String mySelectionStateKey;

  public ShowExecutionErrorsOnlyAction(@NotNull Filterable<ExecutionNode> filterable) {
    this(filterable, "build.toolwindow.show.errors.only.selection.state");
  }

  public ShowExecutionErrorsOnlyAction(@NotNull Filterable<ExecutionNode> filterable, @Nullable String selectionStateKey) {
    super("Show Errors Only", "Show Errors Only", AllIcons.General.Error);
    myFilterable = filterable;
    mySelectionStateKey = selectionStateKey;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    if (!Registry.is("build.view.side-by-side", true)) {
      presentation.setVisible(false);
      return false;
    }
    boolean filteringEnabled = myFilterable.isFilteringEnabled();
    presentation.setEnabledAndVisible(filteringEnabled);
    if (filteringEnabled && mySelectionStateKey != null &&
        PropertiesComponent.getInstance().getBoolean(mySelectionStateKey, true) &&
        myFilterable.getFilter() != ERROR_FILTER) {
      setSelected(e, true);
    }

    return myFilterable.getFilter() == ERROR_FILTER;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    myFilterable.setFilter(state ? ERROR_FILTER : null);
    if (mySelectionStateKey != null) {
      PropertiesComponent.getInstance().setValue(mySelectionStateKey, state, true);
    }
  }
}
