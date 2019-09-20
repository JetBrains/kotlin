// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public interface BuildViewSettingsProvider {
  boolean isExecutionViewHidden();

  default boolean isSideBySideView() {
    return Registry.is("build.view.side-by-side", true);
  }
}
