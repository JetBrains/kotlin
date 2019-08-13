/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.impl;

import com.intellij.openapi.options.CompoundScheme;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TemplateGroup extends CompoundScheme<TemplateImpl> {
  private final String myReplace;

  private boolean isModified = true;

  public boolean isModified() {
    return isModified;
  }

  public void setModified(boolean modified) {
    isModified = modified;
  }

  public TemplateGroup(final String name) {
    this(name, null);
  }

  public TemplateGroup(String name, @Nullable String replace) {
    super(name);
    myReplace = replace;
  }

  public String getReplace() {
    return myReplace;
  }

  public boolean containsTemplate(@NotNull final String key, @Nullable final String id) {
    return ContainerUtil.or(getElements(), template -> key.equals(template.getKey()) || id != null && id.equals(template.getId()));
  }

  @Override
  public String toString() {
    return getName();
  }
}
