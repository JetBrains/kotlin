// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationPanel.ActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * @author Pavel.Dolgov
 */
public interface ProjectSdkSetupValidator {
  ExtensionPointName<ProjectSdkSetupValidator> EP_NAME = ExtensionPointName.create("com.intellij.projectSdkSetupValidator");

  boolean isApplicableFor(@NotNull Project project, @NotNull VirtualFile file);

  @Nullable
  @NlsContexts.Label
  String getErrorMessage(@NotNull Project project, @NotNull VirtualFile file);

  /**
   * @deprecated use {@link #getFixHandler(Project, VirtualFile)} for better presentation
   */
  @Deprecated
  default void doFix(@NotNull Project project, @NotNull VirtualFile file) {
    throw new RuntimeException("Not implemented. Please implement #getFixHandler or this method");
  }

  /**
   * Executed only if {@link #isApplicableFor(Project, VirtualFile)} has returned true.
   * The implementation should only return action handlers without doing anything in the method
   * implementation.
   * @return a handler for the possible invocation of the fix action
   * <br/>
   * NOTE. Do not forget to implement this method.
   */
  @NotNull
  @SuppressWarnings("MissingDeprecatedAnnotation")
  default EditorNotificationPanel.ActionHandler getFixHandler(@NotNull Project project, @NotNull VirtualFile file) {
    return new ActionHandler() {
      @Override
      @Deprecated
      public void handlePanelActionClick(@NotNull EditorNotificationPanel panel,
                                         @NotNull HyperlinkEvent e) {
        doFix(project, file);
      }

      @Override
      @Deprecated
      public void handleQuickFixClick(@NotNull Editor editor, @NotNull PsiFile psiFile) {
        doFix(project, file);
      }
    };
  }
}
