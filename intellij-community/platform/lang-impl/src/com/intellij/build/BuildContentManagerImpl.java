// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.build.process.BuildProcessHandler;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.BaseContentCloseListener;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.TabbedContent;
import com.intellij.util.ContentUtilEx;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.util.ContentUtilEx.getFullName;

/**
 * @author Vladislav.Soroka
 */
public final class BuildContentManagerImpl implements BuildContentManager {
  public static final String Build = "Build";
  public static final String Sync = "Sync";
  public static final String Run = "Run";
  public static final String Debug = "Debug";
  private static final String[] ourPresetOrder = {Sync, Build, Run, Debug};
  private static final Key<Map<Object, CloseListener>> CONTENT_CLOSE_LISTENERS = Key.create("CONTENT_CLOSE_LISTENERS");

  private final Project myProject;
  private final Map<Content, Pair<Icon, AtomicInteger>> liveContentsMap = new ConcurrentHashMap<>();

  public BuildContentManagerImpl(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public @NotNull ToolWindow getOrCreateToolWindow() {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    if (toolWindow != null) {
      return toolWindow;
    }

    toolWindow = toolWindowManager.registerToolWindow(RegisterToolWindowTask.closable(TOOL_WINDOW_ID, AllIcons.Toolwindows.ToolWindowBuild));
    ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addDataProvider(new DataProvider() {
      private int myInsideGetData = 0;

      @Override
      public Object getData(@NotNull String dataId) {
        myInsideGetData++;
        try {
          return myInsideGetData == 1 ? DataManager.getInstance().getDataContext(contentManager.getComponent()).getData(dataId) : null;
        }
        finally {
          myInsideGetData--;
        }
      }
    });

    ContentManagerWatcher.watchContentManager(toolWindow, contentManager);
    return toolWindow;
  }

  private void invokeLaterIfNeeded(@NotNull Runnable runnable) {
    if (myProject.isDefault()) {
      return;
    }
    StartupManagerEx.getInstanceEx(myProject).runAfterOpened(() -> {
      GuiUtils.invokeLaterIfNeeded(runnable, ModalityState.defaultModalityState(), myProject.getDisposed());
    });
  }

  @Override
  public void addContent(Content content) {
    invokeLaterIfNeeded(() -> {
      ContentManager contentManager = getOrCreateToolWindow().getContentManager();
      final String name = content.getTabName();
      final String category = StringUtil.trimEnd(StringUtil.split(name, " ").get(0), ':');
      int idx = -1;
      for (int i = 0; i < ourPresetOrder.length; i++) {
        final String s = ourPresetOrder[i];
        if (s.equals(category)) {
          idx = i;
          break;
        }
      }
      final Content[] existingContents = contentManager.getContents();
      if (idx != -1) {
        MultiMap<String, String> existingCategoriesNames = new MultiMap<>();
        for (Content existingContent : existingContents) {
          String tabName = existingContent.getTabName();
          existingCategoriesNames.putValue(StringUtil.trimEnd(StringUtil.split(tabName, " ").get(0), ':'), tabName);
        }

        int place = 0;
        for (int i = 0; i <= idx; i++) {
          String key = ourPresetOrder[i];
          Collection<String> tabNames = existingCategoriesNames.get(key);
          place += tabNames.size();
        }
        contentManager.addContent(content, place);
      }
      else {
        contentManager.addContent(content);
      }

      for (Content existingContent : existingContents) {
        existingContent.setDisplayName(existingContent.getTabName());
      }
      String tabName = content.getTabName();
      updateTabDisplayName(content, tabName);
    });
  }

  public void updateTabDisplayName(Content content, String tabName) {
    invokeLaterIfNeeded(() -> {
      if (!tabName.equals(content.getDisplayName())) {
        // we are going to adjust display name, so we need to ensure tab name is not retrieved based on display name
        content.setTabName(tabName);
        content.setDisplayName(tabName);
      }
    });
  }

  @Override
  public void removeContent(Content content) {
    invokeLaterIfNeeded(() -> {
      ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(TOOL_WINDOW_ID);
      ContentManager contentManager = toolWindow == null ? null : toolWindow.getContentManager();
      if (contentManager != null && (!contentManager.isDisposed())) {
        contentManager.removeContent(content, true);
      }
    });
  }

  @Override
  public void setSelectedContent(@NotNull Content content,
                                 boolean requestFocus,
                                 boolean forcedFocus,
                                 boolean activate,
                                 @Nullable Runnable activationCallback) {
    invokeLaterIfNeeded(() -> {
      ToolWindow toolWindow = getOrCreateToolWindow();
      if (!toolWindow.isAvailable()) {
        return;
      }
      if (activate) {
        toolWindow.show(activationCallback);
      }
      toolWindow.getContentManager().setSelectedContent(content, requestFocus, forcedFocus, false);
    });
  }

  @Override
  public Content addTabbedContent(@NotNull JComponent contentComponent,
                                  @NotNull String groupPrefix,
                                  @NotNull String tabName,
                                  @Nullable Icon icon,
                                  @Nullable Disposable childDisposable) {
    ContentManager contentManager = getOrCreateToolWindow().getContentManager();
    ContentUtilEx.addTabbedContent(contentManager, contentComponent, groupPrefix, tabName, false, childDisposable);
    Content content = contentManager.findContent(getFullName(groupPrefix, tabName));
    if (icon != null) {
      TabbedContent tabbedContent = ContentUtilEx.findTabbedContent(contentManager, groupPrefix);
      if (tabbedContent != null) {
        tabbedContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        tabbedContent.setIcon(icon);
      }
    }
    return content;
  }

  public void startBuildNotified(@NotNull BuildDescriptor buildDescriptor,
                                 @NotNull Content content,
                                 @Nullable BuildProcessHandler processHandler) {
    if (processHandler != null) {
      Map<Object, CloseListener> closeListenerMap = content.getUserData(CONTENT_CLOSE_LISTENERS);
      if (closeListenerMap == null) {
        closeListenerMap = new HashMap<>();
        content.putUserData(CONTENT_CLOSE_LISTENERS, closeListenerMap);
      }
      closeListenerMap.put(buildDescriptor.getId(), new CloseListener(content, processHandler));
    }
    Pair<Icon, AtomicInteger> pair = liveContentsMap.computeIfAbsent(content, c -> Pair.pair(c.getIcon(), new AtomicInteger(0)));
    pair.second.incrementAndGet();
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
    if (pair.first == null) {
      content.putUserData(Content.TAB_LABEL_ORIENTATION_KEY, ComponentOrientation.RIGHT_TO_LEFT);
    }
    content.setIcon(ExecutionUtil.getLiveIndicator(pair.first, 0, 13));
    invokeLaterIfNeeded(() -> {
      JComponent component = content.getComponent();
      component.invalidate();
      if (!liveContentsMap.isEmpty()) {
        getOrCreateToolWindow().setIcon(ExecutionUtil.getLiveIndicator(AllIcons.Toolwindows.ToolWindowBuild));
      }
    });
  }

  public void finishBuildNotified(@NotNull BuildDescriptor buildDescriptor, @NotNull Content content) {
    Map<Object, CloseListener> closeListenerMap = content.getUserData(CONTENT_CLOSE_LISTENERS);
    if (closeListenerMap != null) {
      CloseListener closeListener = closeListenerMap.remove(buildDescriptor.getId());
      if (closeListener != null) {
        closeListener.dispose();
        if (closeListenerMap.isEmpty()) {
          content.putUserData(CONTENT_CLOSE_LISTENERS, null);
        }
      }
    }

    Pair<Icon, AtomicInteger> pair = liveContentsMap.get(content);
    if (pair != null && pair.second.decrementAndGet() == 0) {
      content.setIcon(pair.first);
      if (pair.first == null) {
        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.FALSE);
      }
      liveContentsMap.remove(content);
    }

    invokeLaterIfNeeded(() -> {
      if (liveContentsMap.isEmpty()) {
        getOrCreateToolWindow().setIcon(AllIcons.Toolwindows.ToolWindowBuild);
      }
    });
  }

  private final class CloseListener extends BaseContentCloseListener {
    private @Nullable BuildProcessHandler myProcessHandler;

    private CloseListener(final @NotNull Content content, @NotNull BuildProcessHandler processHandler) {
      super(content, myProject);
      myProcessHandler = processHandler;
    }

    @Override
    protected void disposeContent(@NotNull Content content) {
      if (myProcessHandler instanceof Disposable) {
        Disposer.dispose((Disposable)myProcessHandler);
      }
      myProcessHandler = null;
    }

    @Override
    protected boolean closeQuery(@NotNull Content content, boolean modal) {
      if (myProcessHandler == null || myProcessHandler.isProcessTerminated() || myProcessHandler.isProcessTerminating()) {
        return true;
      }
      myProcessHandler.putUserData(RunContentManagerImpl.ALWAYS_USE_DEFAULT_STOPPING_BEHAVIOUR_KEY, Boolean.TRUE);
      final String sessionName = myProcessHandler.getExecutionName();
      final WaitForProcessTask task = new WaitForProcessTask(myProcessHandler, sessionName, modal, myProject) {
        @Override
        public void onCancel() {
          // stop waiting for the process
          myProcessHandler.forceProcessDetach();
        }
      };
      return askUserAndWait(myProcessHandler, sessionName, task);
    }
  }
}
