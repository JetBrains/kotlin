// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultExternalTask implements ExternalTask {
  @NotNull
  private String name;
  @NotNull
  private String qName;
  @Nullable
  private String description;
  @Nullable
  private String group;
  @Nullable
  private String type;
  private boolean isTest;

  public DefaultExternalTask() {
  }

  public DefaultExternalTask(@NotNull ExternalTask externalTask) {
    name = externalTask.getName();
    qName = externalTask.getQName();
    description = externalTask.getDescription();
    group = externalTask.getGroup();
    type = externalTask.getType();
    isTest = externalTask.isTest();
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  @NotNull
  @Override
  public String getQName() {
    return qName;
  }

  public void setQName(@NotNull String QName) {
    qName = QName;
  }

  @Nullable
  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Nullable
  @Override
  public String getGroup() {
    return group;
  }

  public void setGroup(@Nullable String group) {
    this.group = group;
  }

  @Nullable
  @Override
  public String getType() {
    return type;
  }

  public void setType(@Nullable String type) {
    this.type = type;
  }

  @Override
  public boolean isTest() {
    return isTest;
  }

  public void setTest(boolean test) {
    isTest = test;
  }
}
