// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.ComponentUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT;

/**
 * @author Sergey.Malenkov
 */
public abstract class NavBarActions extends AnAction implements DumbAware {
  NavBarActions() {
    setEnabledInModalContext(true);
  }

  @Override
  public final void update(@NotNull AnActionEvent event) {
    NavBarPanel panel = ComponentUtil.getParentOfType((Class<? extends NavBarPanel>)NavBarPanel.class, event.getData(CONTEXT_COMPONENT));
    event.getPresentation().setEnabled(panel != null);
  }

  @Override
  public final void actionPerformed(@NotNull AnActionEvent event) {
    NavBarPanel panel = ComponentUtil.getParentOfType((Class<? extends NavBarPanel>)NavBarPanel.class, event.getData(CONTEXT_COMPONENT));
    if (panel != null) actionPerformed(panel);
  }

  abstract void actionPerformed(@NotNull NavBarPanel panel);

  public static final class Home extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.moveHome();
    }
  }

  public static final class End extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.moveEnd();
    }
  }

  public static final class Up extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.moveDown();
    }
  }

  public static final class Down extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.moveDown();
    }
  }

  public static final class Left extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.moveLeft();
    }
  }

  public static final class Right extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.moveRight();
    }
  }

  public static final class Escape extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.escape();
    }
  }

  public static final class Enter extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.enter();
    }
  }

  public static final class Navigate extends NavBarActions {
    @Override
    void actionPerformed(@NotNull NavBarPanel panel) {
      panel.navigate();
    }
  }
}
