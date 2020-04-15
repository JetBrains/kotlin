// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.actions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IntRef;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.find.actions.ShowUsagesAction.getUsagesPageSize;

class ShowUsagesParameters {

  final @NotNull Project project;
  final @Nullable Editor editor;
  final @NotNull RelativePoint popupPosition;
  final @NotNull IntRef minWidth;
  final int maxUsages;

  private ShowUsagesParameters(@NotNull Project project,
                               @Nullable Editor editor,
                               @NotNull RelativePoint popupPosition,
                               @NotNull IntRef minWidth,
                               int maxUsages) {
    this.project = project;
    this.editor = editor;
    this.popupPosition = popupPosition;
    this.minWidth = minWidth;
    this.maxUsages = maxUsages;
  }

  @NotNull ShowUsagesParameters moreUsages() {
    return new ShowUsagesParameters(project, editor, popupPosition, minWidth, maxUsages + getUsagesPageSize());
  }

  @NotNull ShowUsagesParameters withEditor(@NotNull Editor editor) {
    return new ShowUsagesParameters(project, editor, popupPosition, minWidth, maxUsages);
  }

  static @NotNull ShowUsagesParameters initial(@NotNull Project project, @Nullable Editor editor, @NotNull RelativePoint popupPosition) {
    return new ShowUsagesParameters(project, editor, popupPosition, new IntRef(0), getUsagesPageSize());
  }
}
