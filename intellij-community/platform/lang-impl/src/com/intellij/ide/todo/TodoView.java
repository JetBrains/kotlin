// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo;

import com.intellij.ide.IdeBundle;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.IdeUICustomization;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ObjectUtils;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.OptionTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

@State(name = "TodoView", storages = {
  @Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE),
  @Storage(value = StoragePathMacros.WORKSPACE_FILE, deprecated = true)
})
public class TodoView implements PersistentStateComponent<TodoView.State>, Disposable {
  private final Project myProject;

  private ContentManager myContentManager;
  private TodoPanel myAllTodos;
  @Nullable
  private TodoPanel myChangeListTodosPanel;
  private CurrentFileTodosPanel myCurrentFileTodosPanel;
  private ScopeBasedTodosPanel myScopeBasedTodosPanel;
  private final List<TodoPanel> myPanels = new ArrayList<>();
  private final List<Content> myNotAddedContent = new ArrayList<>();

  private State state = new State();

  private final TodoViewChangesSupport myChangesSupport;
  private final TodoViewChangesSupport.Listener myChangesListener;
  private Content myChangeListTodosContent;

  public TodoView(@NotNull Project project) {
    myProject = project;

    state.all.arePackagesShown = true;
    state.all.isAutoScrollToSource = true;

    state.current.isAutoScrollToSource = true;

    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(TodoConfiguration.PROPERTY_CHANGE, new MyPropertyChangeListener());
    connection.subscribe(FileTypeManager.TOPIC, new MyFileTypeListener());

    myChangesSupport = project.getService(TodoViewChangesSupport.class);
    myChangesListener = myChangesSupport.installListener(project, connection,
                                                         () -> { return myContentManager; },
                                                         () -> { return myChangeListTodosContent; });
  }

  static class State {
    @Attribute("selected-index")
    public int selectedIndex;

    @OptionTag(value = "selected-file", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings current = new TodoPanelSettings();

    @OptionTag(value = "all", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings all = new TodoPanelSettings();

    @OptionTag(value = "default-changelist", nameAttribute = "id", tag = "todo-panel", valueAttribute = "")
    public TodoPanelSettings changeList = new TodoPanelSettings();
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  @Override
  public State getState() {
    if (myContentManager != null) {
      // all panel were constructed
      Content content = myContentManager.getSelectedContent();
      state.selectedIndex = content == null ? -1 : myContentManager.getIndexOfContent(content);
    }
    return state;
  }

  @Override
  public void dispose() {
  }

  @TestOnly
  public enum Scope {
    AllTodos,
    ChangeList,
    CurrentFile,
    ScopeBased
  }

  @TestOnly
  public TodoTreeBuilder getBuilderAndAllowUpdatesOnIt(Scope scope) {
    TodoTreeBuilder builder = null;
    switch (scope) {
      case AllTodos:
        builder = myAllTodos.myTodoTreeBuilder;
        break;
      case ChangeList:
        builder = myChangeListTodosPanel.myTodoTreeBuilder;
        break;
      case CurrentFile:
        builder = myCurrentFileTodosPanel.myTodoTreeBuilder;
        break;
      case ScopeBased:
        builder = myScopeBasedTodosPanel.myTodoTreeBuilder;
        break;
    }

    builder.setUpdatable(true);
    return builder;
  }

  public void initToolWindow(@NotNull ToolWindow toolWindow) {
    // Create panels
    ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    Content allTodosContent = contentFactory.createContent(null, IdeUICustomization.getInstance().projectMessage("tab.title.project"), false);
    toolWindow.setHelpId("find.todoList");
    myAllTodos = new TodoPanel(myProject, state.all, false, allTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        AllTodosTreeBuilder builder = createAllTodoBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    allTodosContent.setComponent(wrapWithDumbModeSpoiler(myAllTodos));
    allTodosContent.setPreferredFocusableComponent(myAllTodos.getTree());
    Disposer.register(this, myAllTodos);
    if (toolWindow instanceof ToolWindowEx) {
      DefaultActionGroup group = new DefaultActionGroup() {
        {
          getTemplatePresentation().setText(IdeBundle.messagePointer("group.view.options"));
          setPopup(true);
          add(myAllTodos.createAutoScrollToSourceAction());
          addSeparator();
          addAll(myAllTodos.createGroupByActionGroup());
        }
      };
      ((ToolWindowEx)toolWindow).setAdditionalGearActions(group);
    }

    Content currentFileTodosContent = contentFactory.createContent(null, IdeBundle.message("title.todo.current.file"), false);
    myCurrentFileTodosPanel = new CurrentFileTodosPanel(myProject, state.current, currentFileTodosContent) {
      @Override
      protected TodoTreeBuilder createTreeBuilder(JTree tree, Project project) {
        CurrentFileTodosTreeBuilder builder = new CurrentFileTodosTreeBuilder(tree, project);
        builder.init();
        return builder;
      }
    };
    Disposer.register(this, myCurrentFileTodosPanel);
    currentFileTodosContent.setComponent(wrapWithDumbModeSpoiler(myCurrentFileTodosPanel));
    currentFileTodosContent.setPreferredFocusableComponent(myCurrentFileTodosPanel.getTree());

    String tabName = myChangesSupport.getTabName(myProject);
    myChangeListTodosContent = contentFactory.createContent(null, tabName, false);
    myChangeListTodosPanel = myChangesSupport.createPanel(myProject, state.current, myChangeListTodosContent);
    if (myChangeListTodosPanel != null) {
      Disposer.register(this, myChangeListTodosPanel);
      myChangeListTodosContent.setComponent(wrapWithDumbModeSpoiler(myChangeListTodosPanel));
      myChangeListTodosContent.setPreferredFocusableComponent(myCurrentFileTodosPanel.getTree());
    }

    Content scopeBasedTodoContent = contentFactory.createContent(null, LangBundle.message("tab.title.scope.based"), false);
    myScopeBasedTodosPanel = new ScopeBasedTodosPanel(myProject, state.current, scopeBasedTodoContent);
    Disposer.register(this, myScopeBasedTodosPanel);
    scopeBasedTodoContent.setComponent(wrapWithDumbModeSpoiler(myScopeBasedTodosPanel));

    myContentManager = toolWindow.getContentManager();

    myContentManager.addContent(allTodosContent);
    myContentManager.addContent(currentFileTodosContent);
    myContentManager.addContent(scopeBasedTodoContent);

    if (myChangesSupport.isContentVisible(myProject)) {
      myChangesListener.setVisible(true);
      myContentManager.addContent(myChangeListTodosContent);
    }
    for (Content content : myNotAddedContent) {
      myContentManager.addContent(content);
    }

    myChangeListTodosContent.setCloseable(false);
    allTodosContent.setCloseable(false);
    currentFileTodosContent.setCloseable(false);
    scopeBasedTodoContent.setCloseable(false);
    Content content = myContentManager.getContent(state.selectedIndex);
    myContentManager.setSelectedContent(content == null ? allTodosContent : content);

    myPanels.add(myAllTodos);
    if (myChangeListTodosPanel != null) {
      myPanels.add(myChangeListTodosPanel);
    }
    myPanels.add(myCurrentFileTodosPanel);
    myPanels.add(myScopeBasedTodosPanel);
  }

  @TestOnly
  public void disposePanels() {
    Disposer.dispose(myAllTodos);
    if (myChangeListTodosPanel != null) {
      Disposer.dispose(myChangeListTodosPanel);
    }
    Disposer.dispose(myCurrentFileTodosPanel);
    Disposer.dispose(myScopeBasedTodosPanel);
  }

  @NotNull
  static String getTabNameForChangeList(@NotNull String changelistName) {
    changelistName = changelistName.trim();
    String suffix = "Changelist";
    return StringUtil.endsWithIgnoreCase(changelistName, suffix) ? changelistName : changelistName + " " + suffix;
  }

  @NotNull
  protected AllTodosTreeBuilder createAllTodoBuilder(JTree tree, Project project) {
    return new AllTodosTreeBuilder(tree, project);
  }

  private final class MyPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent e) {
      if (TodoConfiguration.PROP_TODO_PATTERNS.equals(e.getPropertyName()) || TodoConfiguration.PROP_TODO_FILTERS.equals(e.getPropertyName())) {
        updateFilters();
      }
    }

    private void updateFilters() {
      try {
        for (TodoPanel panel : myPanels) {
          panel.updateTodoFilter();
        }
      } catch (ProcessCanceledException ignore) { }
    }
  }

  private final class MyFileTypeListener implements FileTypeListener {
    @Override
    public void fileTypesChanged(@NotNull FileTypeEvent e) {
      refresh();
    }
  }

  public void refresh() {
    Map<TodoPanel, Set<VirtualFile>> files = new HashMap<>();
    ReadAction.nonBlocking(() -> {
      if (myAllTodos == null) {
        return;
      }
      for (TodoPanel panel : myPanels) {
        panel.myTodoTreeBuilder.collectFiles(virtualFile -> {
          files.computeIfAbsent(panel, p -> new HashSet<>()).add(virtualFile);
          return true;
        });
      }
    })
      .finishOnUiThread(ModalityState.NON_MODAL, (__) -> {
        for (TodoPanel panel : myPanels) {
          panel.rebuildCache(ObjectUtils.notNull(files.get(panel), new HashSet<>()));
          panel.updateTree();
          notifyUpdateFinished();
        }
      })
      .inSmartMode(myProject)
      .submit(NonUrgentExecutor.getInstance());
  }

  protected void notifyUpdateFinished() {
    //do nothing
  }

  public void addCustomTodoView(final TodoTreeBuilderFactory factory, final String title, final TodoPanelSettings settings) {
    Content content = ContentFactory.SERVICE.getInstance().createContent(null, title, true);
    final TodoPanel panel = myChangesSupport.createPanel(myProject, settings, content, factory);
    if (panel == null) return;

    content.setComponent(wrapWithDumbModeSpoiler(panel));
    Disposer.register(this, panel);

    if (myContentManager == null) {
      myNotAddedContent.add(content);
    }
    else {
      myContentManager.addContent(content);
    }
    myPanels.add(panel);
    content.setCloseable(true);
    content.setDisposer(new Disposable() {
      @Override
      public void dispose() {
        myPanels.remove(panel);
      }
    });
  }

  @NotNull
  private JComponent wrapWithDumbModeSpoiler(@NotNull TodoPanel panel) {
    return DumbService.getInstance(myProject).wrapWithSpoiler(panel, () -> ApplicationManager.getApplication().invokeLater(() -> {
      panel.rebuildCache();
      panel.updateTree();
    }, myProject.getDisposed()), panel);
  }
}
