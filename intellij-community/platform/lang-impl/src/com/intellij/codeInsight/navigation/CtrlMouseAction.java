// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This interface is expected to be implemented by {@linkplain com.intellij.openapi.actionSystem.AnAction AnAction}.
 */
@ApiStatus.Internal
public interface CtrlMouseAction {

  /**
   * This method is called when some action has {@linkplain java.awt.event.MouseEvent#BUTTON1 BUTTON1} shortcut,
   * and user holds shortcut modifiers without pressing the mouse button.
   * <p/>
   * Example: "Go to Type Declaration" action has ctrl+shift+mouse1 shortcut; the method is called when user holds both ctrl and shift.
   * <p/>
   * This method is called in read action in background thread.
   *
   * @return info instance, which will be used to draw highlighting and show tooltip near the mouse pointer
   */
  @Nullable CtrlMouseInfo getCtrlMouseInfo(@NotNull Editor editor, @NotNull PsiFile file, int offset);
}
