// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author Dmitry Avdeev
 */
public abstract class DependentSdkType extends SdkType {

  public DependentSdkType(@NonNls @NotNull String name) {
    super(name);
  }

  /**
   * Checks if dependencies satisfied.
   */
  protected boolean checkDependency(@NotNull SdkModel sdkModel) {
    return ContainerUtil.find(sdkModel.getSdks(), sdk -> isValidDependency(sdk)) != null;
  }

  protected abstract boolean isValidDependency(@NotNull Sdk sdk);

  @NotNull
  public abstract String getUnsatisfiedDependencyMessage();

  @Override
  public boolean supportsCustomCreateUI() {
    return true;
  }

  @Override
  public void showCustomCreateUI(@NotNull final SdkModel sdkModel, @NotNull JComponent parentComponent, @NotNull final Consumer<Sdk> sdkCreatedCallback) {
    if (!checkDependency(sdkModel)) {
      if (Messages.showOkCancelDialog(parentComponent, getUnsatisfiedDependencyMessage(), "Cannot Create SDK", Messages.getWarningIcon()) != Messages.OK) {
        return;
      }
      if (fixDependency(sdkModel, sdkCreatedCallback) == null) {
        return;
      }
    }

    createSdkOfType(sdkModel, this, sdkCreatedCallback);
  }

  @Override
  @NotNull
  public abstract SdkType getDependencyType();

  protected Sdk fixDependency(@NotNull SdkModel sdkModel, @NotNull Consumer<? super Sdk> sdkCreatedCallback) {
    return createSdkOfType(sdkModel, getDependencyType(), sdkCreatedCallback);
  }

  protected static Sdk createSdkOfType(@NotNull SdkModel sdkModel,
                                       @NotNull SdkType sdkType,
                                       @NotNull Consumer<? super Sdk> sdkCreatedCallback) {
    final Ref<Sdk> result = new Ref<>(null);
    SdkConfigurationUtil.selectSdkHome(sdkType, home -> {
      final ProjectJdkImpl newJdk = SdkConfigurationUtil.createSdk(Arrays.asList(sdkModel.getSdks()), home, sdkType, null, null);

      sdkCreatedCallback.consume(newJdk);
      result.set(newJdk);
    });
    return result.get();
  }
}
