// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.find;

import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 * @deprecated unused
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2020.3")
public class FindProgressIndicator extends BackgroundableProcessIndicator {
  public FindProgressIndicator(@NotNull Project project, @NotNull String scopeString) {
    super(project,
         FindBundle.message("find.progress.searching.message", scopeString),
         new SearchInBackgroundOption(),
         FindBundle.message("find.progress.stop.title"),
         FindBundle.message("find.progress.stop.background.button"), true);
  }
}
