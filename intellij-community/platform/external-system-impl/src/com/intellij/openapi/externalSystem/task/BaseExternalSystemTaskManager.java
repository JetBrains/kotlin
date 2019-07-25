// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.task;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import org.jetbrains.jps.model.java.JdkVersionDetector;

public abstract class BaseExternalSystemTaskManager<S extends ExternalSystemExecutionSettings> implements ExternalSystemTaskManager<S> {
  protected boolean isJdk9orLater( String javaHome) {
    JdkVersionDetector.JdkVersionInfo jdkVersionInfo =
      javaHome == null ? null : JdkVersionDetector.getInstance().detectJdkVersionInfo(javaHome);
    return jdkVersionInfo != null && jdkVersionInfo.version.isAtLeast(9);
  }
}
