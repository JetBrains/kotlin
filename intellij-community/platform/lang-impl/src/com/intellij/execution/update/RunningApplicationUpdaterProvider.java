// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.update;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * By implementing this extension it is possible to provide an updater and make Update Running Application action
 * available for particular running application.
 * The action will be available for user if at least one updater is provided.
 * {@link RunningApplicationUpdater#performUpdate()} will be called on performing the action.
 * Popup with available updaters will be shown at first if there is more then one available updater.
 */
public interface RunningApplicationUpdaterProvider {
  ExtensionPointName<RunningApplicationUpdaterProvider> EP_NAME =
    ExtensionPointName.create("com.intellij.runningApplicationUpdaterProvider");

  @Nullable
  RunningApplicationUpdater createUpdater(@NotNull Project project, @NotNull ProcessHandler process);
}
