/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.fileTemplates.impl;

import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Zhuravlev
 */
public final class CustomFileTemplate extends FileTemplateBase {
  private String myName;
  private String myExtension;

  public CustomFileTemplate(@NotNull String name, @NotNull String extension) {
    myName = name;
    myExtension = extension;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  public void setName(@NotNull String name) {
    myName = name;
  }

  @Override
  @NotNull
  public String getExtension() {
    return myExtension;
  }

  @Override
  public void setExtension(@NotNull String extension) {
    myExtension = extension;
  }

  @Override
  @NotNull
  public String getDescription() {
    return "";  // todo: some default description?
  }

  @Override
  public CustomFileTemplate clone() {
    return (CustomFileTemplate)super.clone();
  }

  @Override
  public boolean isDefault() {
    return false;
  }
}
