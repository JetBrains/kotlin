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
package com.intellij.codeInsight.template;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public interface CustomLiveTemplate {
  ExtensionPointName<CustomLiveTemplate> EP_NAME = ExtensionPointName.create("com.intellij.customLiveTemplate");

  @Nullable
  String computeTemplateKey(@NotNull CustomTemplateCallback callback);

  boolean isApplicable(@NotNull CustomTemplateCallback callback, int offset, boolean wrapping);

  boolean supportsWrapping();

  void expand(@NotNull String key, @NotNull CustomTemplateCallback callback);

  void wrap(@NotNull String selection, @NotNull CustomTemplateCallback callback);

  @NotNull
  String getTitle();

  char getShortcut();
}
