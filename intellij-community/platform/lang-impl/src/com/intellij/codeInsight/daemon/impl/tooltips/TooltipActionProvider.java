// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.tooltips;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.TooltipAction;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Provides actions for error tooltips 
 *
 * @see com.intellij.codeInsight.daemon.impl.DaemonTooltipActionProvider
 */
public interface TooltipActionProvider {
  ExtensionPointName<TooltipActionProvider> EXTENSION_POINT_NAME = ExtensionPointName.create("com.intellij.daemon.tooltipActionProvider");

  String SHOW_FIXES_KEY = "tooltips.show.actions.in.key";
  boolean SHOW_FIXES_DEFAULT_VALUE = true;

  @Nullable
  TooltipAction getTooltipAction(@NotNull final HighlightInfo info, @NotNull Editor editor, @NotNull PsiFile psiFile);


  @Nullable
  static TooltipAction calcTooltipAction(@NotNull final HighlightInfo info, @NotNull Editor editor) {
    if (!Registry.is("ide.tooltip.show.with.actions")) return null;

    Project project = editor.getProject();
    if (project == null) return null;
    
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;
    
    for (TooltipActionProvider extension : EXTENSION_POINT_NAME.getExtensions()) {
      TooltipAction action = extension.getTooltipAction(info, editor, file);
      if (action != null) return action;
    }

    return null;
  }

  static boolean isShowActions() {
    return PropertiesComponent.getInstance().getBoolean(SHOW_FIXES_KEY, SHOW_FIXES_DEFAULT_VALUE);
  }

  static void setShowActions(boolean newValue) {
    PropertiesComponent.getInstance().setValue(SHOW_FIXES_KEY, newValue, SHOW_FIXES_DEFAULT_VALUE);
  }
}
