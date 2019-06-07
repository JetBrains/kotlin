// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultExternalSystemJdkProvider implements ExternalSystemJdkProvider {
  @NotNull
  @Override
  public SdkType getJavaSdkType() {
    return SimpleJavaSdkType.getInstance();
  }

  @NotNull
  @Override
  public Sdk getInternalJdk() {
    final String jdkHome = SystemProperties.getJavaHome();
    SimpleJavaSdkType simpleJavaSdkType = SimpleJavaSdkType.getInstance();
    return simpleJavaSdkType.createJdk(simpleJavaSdkType.suggestSdkName(null, jdkHome), jdkHome);
  }

  @NotNull
  @Override
  public Sdk createJdk(@Nullable String jdkName, @NotNull String homePath) {
    SimpleJavaSdkType simpleJavaSdkType = SimpleJavaSdkType.getInstance();
    String sdkName = jdkName != null ? jdkName : simpleJavaSdkType.suggestSdkName(null, homePath);
    return simpleJavaSdkType.createJdk(sdkName, homePath);
  }
}
