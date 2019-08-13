// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface ExternalSystemJdkProvider {
  static ExternalSystemJdkProvider getInstance() {
    return ServiceManager.getService(ExternalSystemJdkProvider.class);
  }

  @NotNull
  SdkType getJavaSdkType();

  @NotNull
  Sdk getInternalJdk();

  @NotNull
  Sdk createJdk(@Nullable String jdkName, @NotNull String homePath);
}
