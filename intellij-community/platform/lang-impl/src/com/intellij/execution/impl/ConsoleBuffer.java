// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.util.SystemProperties;

public class ConsoleBuffer {
  public static boolean useCycleBuffer() {
    return !"disabled".equalsIgnoreCase(System.getProperty("idea.cycle.buffer.size"));
  }

  public static int getCycleBufferSize() {
    UISettings uiSettings = UISettings.getInstance();
    if (uiSettings.getOverrideConsoleCycleBufferSize()) {
      return Math.min(Integer.MAX_VALUE / 1024, uiSettings.getConsoleCycleBufferSizeKb()) * 1024;
    }
    return getLegacyCycleBufferSize();
  }

  public static int getLegacyCycleBufferSize() {
    return SystemProperties.getIntProperty("idea.cycle.buffer.size", 1024) * 1024;
  }
}
