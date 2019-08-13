// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation;

import com.intellij.navigation.NavigationTarget;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@Experimental
public interface GotoDeclarationProvider {

  void collectTargets(@NotNull Project project,
                      @NotNull Editor editor,
                      @NotNull PsiFile file,
                      @NotNull Consumer<? super NavigationTarget> consumer);
}
