// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.util.registry;

import com.intellij.openapi.application.ExperimentalFeature;
import com.intellij.openapi.application.Experiments;
import org.jetbrains.annotations.NotNull;

import java.util.MissingResourceException;

/**
 * @author Konstantin Bulenkov
 */
final class ExperimentalFeatureRegistryValueWrapper extends RegistryValue {
  private final ExperimentalFeature myFeature;

  ExperimentalFeatureRegistryValueWrapper(@NotNull ExperimentalFeature feature) {
    super(Registry.getInstance(), feature.id, null);
    myFeature = feature;
  }

  @NotNull
  @Override
  public String getKey() {
    return myFeature.id;
  }

  @Override
  protected String get(@NotNull String key, String defaultValue, boolean isValue) throws MissingResourceException {
    return asString();
  }

  @NotNull
  @Override
  public String asString() {
    return Boolean.toString(asBoolean());
  }

  @Override
  public boolean asBoolean() {
    return Experiments.getInstance().isFeatureEnabled(myFeature.id);
  }

  @Override
  boolean isRestartRequired() {
    return false;
  }

  @Override
  public boolean isChangedFromDefault() {
    return Experiments.getInstance().isChanged(myFeature.id);
  }

  @Override
  public boolean isBoolean() {
    return true;
  }

  @Override
  public void setValue(String value) {
    boolean enable = Boolean.parseBoolean(value);
    Experiments.getInstance().setFeatureEnabled(myFeature.id, enable);
  }

  @NotNull
  @Override
  public String getDescription() {
    return myFeature.description;
  }
}
