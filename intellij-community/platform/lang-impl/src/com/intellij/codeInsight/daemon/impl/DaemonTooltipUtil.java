// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.hint.TooltipController;
import com.intellij.codeInsight.hint.TooltipGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorMouseHoverPopupManager;
import org.jetbrains.annotations.NotNull;

public final class DaemonTooltipUtil {
  private static final TooltipGroup DAEMON_INFO_GROUP = new TooltipGroup("DAEMON_INFO_GROUP", 0);

  public static void showInfoTooltip(HighlightInfo info, Editor editor, int defaultOffset) {
    showInfoTooltip(info, editor, defaultOffset, false, false);
  }

  public static void cancelTooltips() {
    TooltipController.getInstance().cancelTooltip(DAEMON_INFO_GROUP, null, true);
  }


  static void showInfoTooltip(@NotNull final HighlightInfo info,
                              @NotNull Editor editor,
                              final int defaultOffset,
                              final boolean requestFocus,
                              final boolean showImmediately) {
    EditorMouseHoverPopupManager.getInstance().showInfoTooltip(editor, info, defaultOffset, requestFocus, showImmediately);
  }
}
