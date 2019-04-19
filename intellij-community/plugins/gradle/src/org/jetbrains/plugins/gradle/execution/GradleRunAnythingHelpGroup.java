// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider;
import com.intellij.ide.actions.runAnything.groups.RunAnythingHelpGroup;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class GradleRunAnythingHelpGroup extends RunAnythingHelpGroup {
  @NotNull
  @Override
  public String getTitle() {
    return "Gradle";
  }

  @NotNull
  @Override
  public Collection<RunAnythingProvider> getProviders() {
    return ContainerUtil.immutableSingletonList(RunAnythingProvider.EP_NAME.findExtension(GradleRunAnythingProvider.class));
  }
}
