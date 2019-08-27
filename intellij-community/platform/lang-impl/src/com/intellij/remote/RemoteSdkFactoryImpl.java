// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.remote;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author traff
 */
public abstract class RemoteSdkFactoryImpl<T extends RemoteSdkAdditionalData> implements RemoteSdkFactory<T> {
  @Override
  public Sdk createRemoteSdk(@Nullable Project project, @NotNull T data, @Nullable String sdkName, Collection<Sdk> existingSdks)
    throws RemoteSdkException {
    final String interpreterVersion = getInterpreterVersion(project, data);

    final String name;
    if (StringUtil.isNotEmpty(sdkName)) {
      name = sdkName;
    }
    else {
      name = getInterpreterName(data, interpreterVersion);
    }

    final SdkType sdkType = getSdkType(data);

    final ProjectJdkImpl sdk = SdkConfigurationUtil.createSdk(existingSdks, generateSdkHomePath(data), sdkType, data, name);

    sdk.setVersionString(interpreterVersion);

    data.setValid(true);

    return sdk;
  }

  @Override
  public String generateSdkHomePath(@NotNull T data) {
    return data.getSdkId();
  }

  @NotNull
  protected abstract SdkType getSdkType(@NotNull T data);

  @NotNull
  protected abstract String getInterpreterName(@NotNull T data, @Nullable String version) throws RemoteSdkException;

  @Nullable
  protected abstract String getInterpreterVersion(Project project, @NotNull T data) throws RemoteSdkException;

  @Override
  @NotNull
  public Sdk createUnfinished(T data, Collection<Sdk> existingSdks) {
    final String name = getDefaultUnfinishedName();

    final SdkType sdkType = getSdkType(data);

    final ProjectJdkImpl sdk = SdkConfigurationUtil.createSdk(existingSdks, generateSdkHomePath(data), sdkType, data, name);

    data.setValid(false);

    return sdk;
  }

  @Override
  public String getDefaultUnfinishedName() {
    return "Remote " + sdkName() + " interpreter";
  }


  @Override
  public boolean canSaveUnfinished() {
    return false;
  }
}
