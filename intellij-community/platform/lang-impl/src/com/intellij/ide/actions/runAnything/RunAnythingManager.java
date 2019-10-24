// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.actions.BigPopupUI;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.WindowStateService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBInsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class RunAnythingManager {
  private static final String LOCATION_SETTINGS_KEY = "run.anything.popup";
  private final Project myProject;
  private JBPopup myBalloon;
  private RunAnythingPopupUI myRunAnythingUI;
  private Dimension myBalloonFullSize;
  @Nullable private String mySelectedText;

  public RunAnythingManager(@NotNull Project project) {
    myProject = project;
  }

  public static RunAnythingManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, RunAnythingManager.class);
  }

  public void show(@Nullable String searchText, @NotNull AnActionEvent initEvent) {
    show(searchText, true, initEvent);
  }

  public void show(@Nullable String searchText, boolean selectSearchText, @NotNull AnActionEvent initEvent) {
    IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);

    Project project = initEvent.getProject();

    myRunAnythingUI = createView(initEvent);

    if (searchText != null && !searchText.isEmpty()) {
      myRunAnythingUI.getSearchField().setText(searchText);
    }

    predefineSelectedText(searchText);

    if (selectSearchText) {
      myRunAnythingUI.getSearchField().selectAll();
    }

    myBalloon = JBPopupFactory.getInstance().createComponentPopupBuilder(myRunAnythingUI, myRunAnythingUI.getSearchField())
      .setProject(myProject)
      .setModalContext(false)
      .setCancelOnClickOutside(true)
      .setRequestFocus(true)
      .setCancelKeyEnabled(false)
      .setCancelCallback(() -> {
        saveSearchText();
        return true;
      })
      .addUserData("SIMPLE_WINDOW")
      .setResizable(true)
      .setMovable(true)
      .setDimensionServiceKey(myProject, LOCATION_SETTINGS_KEY, true)
      .setLocateWithinScreenBounds(false)
      .createPopup();
    Disposer.register(myBalloon, myRunAnythingUI);
    if (project != null) {
      Disposer.register(project, myBalloon);
    }

    Dimension size = myRunAnythingUI.getMinimumSize();
    JBInsets.addTo(size, myBalloon.getContent().getInsets());
    myBalloon.setMinimumSize(size);

    Disposer.register(myBalloon, () -> {
      saveSize();
      myRunAnythingUI = null;
      myBalloon = null;
      myBalloonFullSize = null;
    });

    if (myRunAnythingUI.getViewType() == RunAnythingPopupUI.ViewType.SHORT) {
      myBalloonFullSize = WindowStateService.getInstance(myProject).getSize(LOCATION_SETTINGS_KEY);
      Dimension prefSize = myRunAnythingUI.getPreferredSize();
      myBalloon.setSize(prefSize);
    }
    calcPositionAndShow(project, myBalloon);
  }

  private void predefineSelectedText(@Nullable String searchText) {
    if (StringUtil.isEmpty(searchText)) {
      searchText = mySelectedText;
    }

    if (StringUtil.isNotEmpty(searchText)) {
      myRunAnythingUI.getSearchField().setText(searchText);
    }
  }

  private void saveSearchText() {
    if (!isShown()) {
      return;
    }

    mySelectedText = myRunAnythingUI.getSearchField().getText();
  }

  private void calcPositionAndShow(Project project, JBPopup balloon) {
    Point savedLocation = WindowStateService.getInstance(myProject).getLocation(LOCATION_SETTINGS_KEY);

    if (project != null) {
      balloon.showCenteredInCurrentWindow(project);
    }
    else {
      balloon.showInFocusCenter();
    }

    //for first show and short mode popup should be shifted to the top screen half
    if (savedLocation == null && myRunAnythingUI.getViewType() == RunAnythingPopupUI.ViewType.SHORT) {
      Point location = balloon.getLocationOnScreen();
      location.y /= 2;
      balloon.setLocation(location);
    }
  }

  public boolean isShown() {
    return myRunAnythingUI != null && myBalloon != null && !myBalloon.isDisposed();
  }

  @SuppressWarnings("Duplicates")
  @NotNull
  private RunAnythingPopupUI createView(@NotNull AnActionEvent event) {
    RunAnythingPopupUI view = new RunAnythingPopupUI(event);

    view.setSearchFinishedHandler(() -> {
      if (isShown()) {
        myBalloon.cancel();
      }
    });

    view.addViewTypeListener(viewType -> {
      if (!isShown()) {
        return;
      }

      ApplicationManager.getApplication().invokeLater(() -> {
        Dimension minSize = view.getMinimumSize();
        JBInsets.addTo(minSize, myBalloon.getContent().getInsets());
        myBalloon.setMinimumSize(minSize);

        if (viewType == BigPopupUI.ViewType.SHORT) {
          myBalloonFullSize = myBalloon.getSize();
          JBInsets.removeFrom(myBalloonFullSize, myBalloon.getContent().getInsets());
          myBalloon.pack(false, true);
        }
        else {
          if (myBalloonFullSize == null) {
            myBalloonFullSize = view.getPreferredSize();
            JBInsets.addTo(myBalloonFullSize, myBalloon.getContent().getInsets());
          }
          myBalloonFullSize.height = Integer.max(myBalloonFullSize.height, minSize.height);
          myBalloonFullSize.width = Integer.max(myBalloonFullSize.width, minSize.width);

          myBalloon.setSize(myBalloonFullSize);
        }
      });
    });

    return view;
  }

  private void saveSize() {
    if (myRunAnythingUI.getViewType() == RunAnythingPopupUI.ViewType.SHORT) {
      WindowStateService.getInstance(myProject).putSize(LOCATION_SETTINGS_KEY, myBalloonFullSize);
    }
  }
}
