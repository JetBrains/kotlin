// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.fileTemplates.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

@SuppressWarnings("ComponentNotRegistered")
public class CreateFromTemplateAction extends CreateFromTemplateActionBase {
  private final Supplier<? extends FileTemplate> myTemplate;

  /** Avoid calling the constructor from normal IDE actions, because:
   *  - Normal actions are preloaded at startup
   *  - Accessing FileTemplate out of FileTemplateManager triggers costly initialization
   */
  public CreateFromTemplateAction(@NotNull FileTemplate template) {
    this(template.getName(), FileTemplateUtil.getIcon(template), () -> template);
  }

  public CreateFromTemplateAction(String templateName, @Nullable Icon icon, @NotNull Supplier<? extends FileTemplate> template){
    super(templateName, null, icon);
    myTemplate = template;
  }

  @Override
  protected FileTemplate getTemplate(final Project project, final PsiDirectory dir) {
    return myTemplate.get();
  }

  @Override
  public void update(@NotNull AnActionEvent e){
    super.update(e);
    Presentation presentation = e.getPresentation();
    boolean isEnabled = CreateFromTemplateGroup.canCreateFromTemplate(e, myTemplate.get());
    presentation.setEnabledAndVisible(isEnabled);
  }

  @NotNull
  public FileTemplate getTemplate() {
    return myTemplate.get();
  }
}
