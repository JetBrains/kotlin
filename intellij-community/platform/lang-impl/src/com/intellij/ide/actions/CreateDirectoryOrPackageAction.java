// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

public class CreateDirectoryOrPackageAction extends AnAction implements DumbAware {
  public CreateDirectoryOrPackageAction() {
    super(IdeBundle.message("action.create.new.directory.or.package"), IdeBundle.message("action.create.new.directory.or.package"), null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    final Project project = event.getData(CommonDataKeys.PROJECT);
    if (view == null || project == null) return;

    final PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);
    if (directory == null) return;

    final CreateGroupHandler validator;
    final String message, title;

    if (PsiDirectoryFactory.getInstance(project).isPackage(directory)) {
      validator = new CreatePackageHandler(project, directory);
      message = IdeBundle.message("prompt.enter.new.package.name");
      title = IdeBundle.message("title.new.package");
    }
    else {
      validator = new CreateDirectoryHandler(project, directory);
      message = IdeBundle.message("prompt.enter.new.directory.name");
      title = IdeBundle.message("title.new.directory");
    }

    String initialText = validator.getInitialText();
    Messages.showInputDialog(project, message, title, null, initialText, validator, TextRange.from(initialText.length(), 0));

    final PsiElement result = validator.getCreatedElement();
    if (result != null) {
      view.selectElement(result);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();

    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    IdeView view = event.getData(LangDataKeys.IDE_VIEW);
    if (view == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    final PsiDirectory[] directories = view.getDirectories();
    if (directories.length == 0) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    presentation.setEnabledAndVisible(true);

    boolean isPackage = false;
    final PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(project);
    for (PsiDirectory directory : directories) {
      if (factory.isPackage(directory)) {
        isPackage = true;
        break;
      }
    }

    if (isPackage) {
      presentation.setText(IdeBundle.message("action.package"));
      presentation.setIcon(PlatformIcons.PACKAGE_ICON);
    }
    else {
      presentation.setText(IdeBundle.message("action.directory"));
      presentation.setIcon(PlatformIcons.FOLDER_ICON);
    }
  }
}
