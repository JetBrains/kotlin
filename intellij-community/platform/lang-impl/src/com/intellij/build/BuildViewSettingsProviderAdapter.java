// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

public class BuildViewSettingsProviderAdapter implements BuildViewSettingsProvider {
  private boolean myExecutionViewHidden;

  public BuildViewSettingsProviderAdapter(BuildViewSettingsProvider buildViewSettingsProvider) {
    myExecutionViewHidden = buildViewSettingsProvider.isExecutionViewHidden();
  }

  @Override
  public boolean isExecutionViewHidden() {
    return myExecutionViewHidden;
  }
}
