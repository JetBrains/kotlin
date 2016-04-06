/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.android.inspections.klint;

import com.intellij.codeInspection.HTMLComposer;
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension;
import com.intellij.codeInspection.lang.HTMLComposerExtension;
import com.intellij.codeInspection.lang.InspectionExtensionsFactory;
import com.intellij.codeInspection.lang.RefManagerExtension;
import com.intellij.codeInspection.reference.RefManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidInspectionExtensionsFactory extends InspectionExtensionsFactory {
  @Override
  public GlobalInspectionContextExtension createGlobalInspectionContextExtension() {
    return new AndroidLintGlobalInspectionContext();
  }

  @Nullable
  @Override
  public RefManagerExtension createRefManagerExtension(RefManager refManager) {
    return null;
  }

  @Nullable
  @Override
  public HTMLComposerExtension createHTMLComposerExtension(HTMLComposer composer) {
    return null;
  }

  @Override
  public boolean isToCheckMember(@NotNull PsiElement element, @NotNull String id) {
    return true;
  }

  @Override
  public String getSuppressedInspectionIdsIn(@NotNull PsiElement element) {
    return null;
  }

  @Override
  public boolean isProjectConfiguredToRunInspections(@NotNull Project project, boolean online) {
    return true;
  }
}
