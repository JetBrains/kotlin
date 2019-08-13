/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.openapi.compiler.util;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.InspectionToolProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author peter
 */
public abstract class InspectionValidator {
  public static final ExtensionPointName<InspectionValidator> EP_NAME =
    ExtensionPointName.create("com.intellij.compiler.inspectionValidator");
  private final String myDescription;
  private final String myProgressIndicatorText;

  @Nullable
  private final Class<? extends LocalInspectionTool>[] myInspectionToolClasses;

  @Nullable
  private final InspectionToolProvider myInspectionToolProvider;

  protected InspectionValidator(@NotNull final String description, @NotNull final String progressIndicatorText) {
    myDescription = description;
    myProgressIndicatorText = progressIndicatorText;
    myInspectionToolClasses = null;
    myInspectionToolProvider = null;
  }

  /**
   * @deprecated Provide inspection classes via {@link #getInspectionToolClasses(CompileContext)} instead.
   */
  @Deprecated
  @SafeVarargs
  protected InspectionValidator(@NotNull final String description,
                                @NotNull final String progressIndicatorText,
                                final Class<? extends LocalInspectionTool>... inspectionToolClasses) {
    myDescription = description;
    myProgressIndicatorText = progressIndicatorText;
    myInspectionToolClasses = inspectionToolClasses;
    myInspectionToolProvider = null;
  }

  protected InspectionValidator(@NotNull final String description,
                                @NotNull final String progressIndicatorText,
                                final InspectionToolProvider provider) {
    myDescription = description;
    myProgressIndicatorText = progressIndicatorText;
    myInspectionToolClasses = null;
    myInspectionToolProvider = provider;
  }

  protected InspectionValidator(@NotNull final String description,
                                @NotNull final String progressIndicatorText,
                                final Class<? extends InspectionToolProvider> providerClass)
    throws IllegalAccessException, InstantiationException {
    this(description, progressIndicatorText, providerClass.newInstance());
  }


  public abstract boolean isAvailableOnScope(@NotNull CompileScope scope);

  public abstract Collection<VirtualFile> getFilesToProcess(final Project project, final CompileContext context);

  @NotNull
  public Collection<? extends PsiElement> getDependencies(final PsiFile psiFile) {
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  @NotNull
  public Class<? extends LocalInspectionTool>[] getInspectionToolClasses(final CompileContext context) {
    if (myInspectionToolClasses != null) {
      return myInspectionToolClasses;
    }

    assert myInspectionToolProvider != null : "getInspectionToolClasses() must be overridden";
    return myInspectionToolProvider.getInspectionClasses();
  }

  public final String getDescription() {
    return myDescription;
  }

  public final String getProgressIndicatorText() {
    return myProgressIndicatorText;
  }

  public CompilerMessageCategory getCategoryByHighlightDisplayLevel(@NotNull final HighlightDisplayLevel severity,
                                                                    @NotNull final VirtualFile virtualFile,
                                                                    @NotNull final CompileContext context) {
    if (severity == HighlightDisplayLevel.ERROR) return CompilerMessageCategory.ERROR;
    if (severity == HighlightDisplayLevel.WARNING) return CompilerMessageCategory.WARNING;
    return CompilerMessageCategory.INFORMATION;
  }

  @NotNull
  public Map<ProblemDescriptor, HighlightDisplayLevel> checkAdditionally(PsiFile file) {
    return Collections.emptyMap();
  }
}
