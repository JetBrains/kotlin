// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.serialization.internal.adapter;

import org.gradle.api.JavaVersion;
import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;

public class InternalIdeaJavaLanguageSettings implements IdeaJavaLanguageSettings {
  private JavaVersion languageLevel;
  private JavaVersion targetBytecodeVersion;
  private InternalInstalledJdk jdk;

  @Override
  public JavaVersion getLanguageLevel() {
    return this.languageLevel;
  }

  public void setLanguageLevel(JavaVersion languageLevel) {
    this.languageLevel = languageLevel;
  }

  @Override
  public JavaVersion getTargetBytecodeVersion() {
    return this.targetBytecodeVersion;
  }

  public void setTargetBytecodeVersion(JavaVersion targetBytecodeVersion) {
    this.targetBytecodeVersion = targetBytecodeVersion;
  }

  @Override
  public InternalInstalledJdk getJdk() {
    return this.jdk;
  }

  public void setJdk(InternalInstalledJdk jdk) {
    this.jdk = jdk;
  }
}
