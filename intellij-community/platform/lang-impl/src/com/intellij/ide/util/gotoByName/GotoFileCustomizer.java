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
package com.intellij.ide.util.gotoByName;

import com.intellij.ide.actions.GotoFileItemProvider;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows to customize item provider used by "Go to | File" action
 */
public interface GotoFileCustomizer {

  ExtensionPointName<GotoFileCustomizer> EP_NAME = ExtensionPointName.create("com.intellij.gotoFileCustomizer");

  /**
   * Creates a GotoFileItemProvider used by "Go to | File" action to collect items
   */
  @Nullable
  GotoFileItemProvider createItemProvider(@NotNull Project project, @Nullable PsiElement context, GotoFileModel model);
}
