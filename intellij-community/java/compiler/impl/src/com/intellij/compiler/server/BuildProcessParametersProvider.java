// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server;

import com.intellij.openapi.extensions.ProjectExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Project-level extension point to dynamically vary build process parameters like classpath, bootclasspath and JVM arguments.
 * @see CompileServerPlugin
 */
public abstract class BuildProcessParametersProvider {
  public static final ProjectExtensionPointName<BuildProcessParametersProvider>
    EP_NAME = new ProjectExtensionPointName<>("com.intellij.buildProcess.parametersProvider");

  /**
   * Override this method to include additional jars to the build process classpath
   * @return list of paths to additional jars to be included to the build process classpath
   */
  public @NotNull List<String> getClassPath() {
    return Collections.emptyList();
  }

  /**
   * Override this method to include additional jars to the build process launcher classpath. This may be needed if the plugin provides
   * custom implementation of Java compiler which must be loaded by the same classloader as tools.jar
   * @return list of paths to additional jars to be included to the build process launcher classpath
   */
  public @NotNull List<String> getLauncherClassPath() {
    return Collections.emptyList();
  }

  public @NotNull List<String> getVMArguments() {
    return Collections.emptyList();
  }

  public boolean isProcessPreloadingEnabled() {
    return true;
  }
}
