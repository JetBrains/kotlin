// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public interface RunConfigurationConverter {
  /**
   * Returns true if applicable, false otherwise
   */
  boolean convertRunConfigurationOnDemand(@NotNull Element element);
}
