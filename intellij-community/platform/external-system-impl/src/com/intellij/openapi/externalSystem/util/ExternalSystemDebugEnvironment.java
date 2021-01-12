// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util;

import org.jetbrains.annotations.NonNls;

/**
 * @author Vladislav.Soroka
 */
public final class ExternalSystemDebugEnvironment {

  @NonNls public static final boolean DEBUG_ORPHAN_MODULES_PROCESSING =
    Boolean.getBoolean("external.system.debug.orphan.modules.processing");

  private ExternalSystemDebugEnvironment() {
  }
}
