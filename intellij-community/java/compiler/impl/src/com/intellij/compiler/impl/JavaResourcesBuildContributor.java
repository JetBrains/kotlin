// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType;
import org.jetbrains.jps.builders.java.ResourcesTargetType;

import java.util.List;

/**
 * @author Konstantin Aleev
 */
public class JavaResourcesBuildContributor implements UpdateResourcesBuildContributor {
  @Override
  @NotNull
  public List<? extends ModuleBasedBuildTargetType<?>> getResourceTargetTypes() {
    return ResourcesTargetType.ALL_TYPES;
  }
}
