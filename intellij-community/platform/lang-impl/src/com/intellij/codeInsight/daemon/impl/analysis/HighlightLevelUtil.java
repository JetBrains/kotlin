// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl.analysis;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public final class HighlightLevelUtil {
  private HighlightLevelUtil() {
  }

  public static void forceRootHighlighting(@NotNull PsiElement root, @NotNull FileHighlightingSetting level) {
    final HighlightingSettingsPerFile component = HighlightingSettingsPerFile.getInstance(root.getProject());
    if (component == null) return;

    component.setHighlightingSettingForRoot(root, level);
  }
}
