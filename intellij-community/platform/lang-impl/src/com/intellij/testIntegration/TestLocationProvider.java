// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testIntegration;

import com.intellij.execution.Location;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** @deprecated override SMTRunnerConsoleProperties.getTestLocator() instead */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
public interface TestLocationProvider {
  ExtensionPointName<TestLocationProvider> EP_NAME = ExtensionPointName.create("com.intellij.testSrcLocator");

  @NotNull
  @SuppressWarnings("rawtypes")
  List<Location> getLocation(@NotNull String protocolId, @NotNull String locationData, Project project);
}