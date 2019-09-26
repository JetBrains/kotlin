// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.executors.DefaultRunExecutor;

public interface BeforeRunTaskAwareConfiguration {

  /**
   * Forces using {@link DefaultRunExecutor} when running this configuration as before run task.
   */
  default boolean useRunExecutor() {
    return false;
  }
}
