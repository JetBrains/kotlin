// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo;

import com.intellij.ConfigurableFactory;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
* @author irengrig
 *         moved from inner class
*/
public class SetTodoFilterAction extends AnAction implements CustomComponentAction {
  private final Project myProject;
  private final TodoPanelSettings myToDoSettings;
  private final Consumer<? super TodoFilter> myTodoFilterConsumer;

  public SetTodoFilterAction(final Project project, final TodoPanelSettings toDoSettings, final Consumer<? super TodoFilter> todoFilterConsumer) {
    super(IdeBundle.message("action.filter.todo.items"), null, AllIcons.General.Filter);
    myProject = project;
    myToDoSettings = toDoSettings;
    myTodoFilterConsumer = todoFilterConsumer;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    JComponent button = presentation.getClientProperty(COMPONENT_KEY);
    DefaultActionGroup group = createPopupActionGroup(myProject, myToDoSettings, myTodoFilterConsumer);
    ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TODO_VIEW_TOOLBAR,
                                                                                  group);
    popupMenu.getComponent().show(button, button.getWidth(), 0);
  }

  @NotNull
  @Override
  public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
    return new ActionButton(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
  }

  public static DefaultActionGroup createPopupActionGroup(final Project project,
                                                          final TodoPanelSettings settings,
                                                          Consumer<? super TodoFilter> todoFilterConsumer) {
    TodoFilter[] filters = TodoConfiguration.getInstance().getTodoFilters();
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new TodoFilterApplier(IdeBundle.message("action.todo.show.all"),
                                    IdeBundle.message("action.description.todo.show.all"), null, settings, todoFilterConsumer));
    for (TodoFilter filter : filters) {
      group.add(new TodoFilterApplier(filter.getName(), null, filter, settings, todoFilterConsumer));
    }
    group.addSeparator();
    group.add(
      new AnAction(IdeBundle.message("action.todo.edit.filters"),
                   IdeBundle.message("action.todo.edit.filters"), AllIcons.General.Settings) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
          util.editConfigurable(project, ConfigurableFactory.Companion.getInstance().getTodoConfigurable(project));
        }
      }
    );
    return group;
  }

  private static class TodoFilterApplier extends ToggleAction {
    private final TodoFilter myFilter;
    private final TodoPanelSettings mySettings;
    private final Consumer<? super TodoFilter> myTodoFilterConsumer;

    /**
     * @param text        action's text.
     * @param description action's description.
     * @param filter      filter to be applied. {@code null} value means "empty" filter.
     * @param settings
     * @param todoFilterConsumer
     */
    TodoFilterApplier(String text,
                      String description,
                      TodoFilter filter,
                      TodoPanelSettings settings,
                      Consumer<? super TodoFilter> todoFilterConsumer) {
      super(null, description, null);
      mySettings = settings;
      myTodoFilterConsumer = todoFilterConsumer;
      getTemplatePresentation().setText(text, false);
      myFilter = filter;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      if (myFilter != null) {
        e.getPresentation().setEnabled(!myFilter.isEmpty());
      }
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      return Comparing.equal(myFilter != null ? myFilter.getName() : null, mySettings.todoFilterName);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      if (state) {
        myTodoFilterConsumer.consume(myFilter);
        //setTodoFilter(myFilter);
      }
    }
  }
}
