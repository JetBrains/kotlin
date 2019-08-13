// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bootRuntime.command;

import org.jetbrains.annotations.NotNull;

public class Processor {

  public static void process(@NotNull RuntimeCommand... commands) {
    for (RuntimeCommand c : commands) {
      c.actionPerformed(null);
    }
  }
}
