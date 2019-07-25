// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.plugins.gradle.internal.daemon.GradleDaemonServices;

/**
 * @author Vladislav.Soroka
 */
public class GradleCleanupService implements Disposable {
  private static final Logger LOG = Logger.getInstance(GradleCleanupService.class);
  // todo should be replaced when it will be possible to distinguish active gradle daemon processes used by other clients
  private static final boolean DISABLE_DAEMONS_STOP = Boolean.getBoolean("idea.gradle.disableDaemonsStopOnExit");

  @Override
  public void dispose() {
    if (ApplicationManager.getApplication().isUnitTestMode() || DISABLE_DAEMONS_STOP) return;
    // do not use DefaultGradleConnector.close() it sends org.gradle.launcher.daemon.protocol.StopWhenIdle message and waits
    try {
      GradleDaemonServices.stopDaemons();
    }
    catch (Exception e) {
      LOG.warn("Failed to stop Gradle daemons during IDE shutdown", e);
    }
  }
}
