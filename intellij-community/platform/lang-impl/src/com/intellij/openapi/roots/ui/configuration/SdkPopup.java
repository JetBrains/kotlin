// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public interface SdkPopup {
  void showPopup(@NotNull AnActionEvent e);
  void showUnderneathToTheRightOf(@NotNull Component component);

  interface SdkPopupListener {
    /**
     * Executed on popup is closed, independently from the result
     */
    default void onClosed() {}

    /**
     * Executed when a new item was created via a user action
     * and added to the model, called after model is refreshed
     */
    default void onNewItemAdded(@NotNull SdkListItem item) {}

    /**
     * Executed when an existing selectable item was selected
     * in the popup, it does mean no new items were created
     * by a user
     */
    default void onExistingItemSelected(@NotNull SdkListItem item) {}
  }
}
