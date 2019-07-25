// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.ModuleBasedBuildTargetType;

import java.util.List;

/**
 * @author nik
 */
public interface UpdateResourcesBuildContributor {
  ExtensionPointName<UpdateResourcesBuildContributor> EP_NAME =
    ExtensionPointName.create("com.intellij.compiler.updateResourcesBuildContributor");

  /**
   * @return list of target types which should be recompiled when 'update resources' task is invoked
   */
  @NotNull
  List<? extends ModuleBasedBuildTargetType<?>> getResourceTargetTypes();
}
