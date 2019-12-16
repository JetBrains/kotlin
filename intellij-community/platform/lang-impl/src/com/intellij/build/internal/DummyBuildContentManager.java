// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.internal;

import com.intellij.build.BuildContentManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public final class DummyBuildContentManager implements BuildContentManager {
  @Override
  public void addContent(Content content) {
  }

  @NotNull
  @Override
  public ToolWindow getOrCreateToolWindow() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeContent(Content content) {
  }

  @Override
  public void setSelectedContent(Content content,
                                 boolean requestFocus,
                                 boolean forcedFocus,
                                 boolean activate,
                                 Runnable activationCallback) {
  }

  @Override
  public Content addTabbedContent(@NotNull JComponent contentComponent,
                                  @NotNull String groupPrefix,
                                  @NotNull String tabName,
                                  @Nullable Icon icon,
                                  @Nullable Disposable childDisposable) {
    return new ContentImpl(contentComponent, tabName, false);
  }
}
