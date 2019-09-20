// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build;

import com.intellij.compiler.impl.UpdateResourcesBuildContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType;
import org.jetbrains.jps.gradle.model.impl.GradleResourcesTargetType;

import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class GradleUpdateResourcesBuildContributor implements UpdateResourcesBuildContributor {
  @Override
  @NotNull
  public List<? extends ModuleBasedBuildTargetType<?>> getResourceTargetTypes() {
    return Arrays.asList(GradleResourcesTargetType.PRODUCTION, GradleResourcesTargetType.TEST);
  }
}
