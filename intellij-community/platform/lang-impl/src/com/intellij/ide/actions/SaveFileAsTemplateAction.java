// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.impl.FileTemplateConfigurable;
import com.intellij.ide.fileTemplates.impl.FileTemplateManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static com.intellij.util.ObjectUtils.assertNotNull;

public class SaveFileAsTemplateAction extends AnAction{
  @Override
  public void actionPerformed(@NotNull AnActionEvent e){
    Project project = assertNotNull(e.getData(CommonDataKeys.PROJECT));
    String fileText = assertNotNull(e.getData(PlatformDataKeys.FILE_TEXT));
    VirtualFile file = assertNotNull(e.getData(CommonDataKeys.VIRTUAL_FILE));
    String extension = StringUtil.notNullize(file.getExtension());
    String nameWithoutExtension = file.getNameWithoutExtension();
    PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
    for(SaveFileAsTemplateHandler handler: SaveFileAsTemplateHandler.EP_NAME.getExtensionList()) {
      String textFromHandler = handler.getTemplateText(psiFile, fileText, nameWithoutExtension);
      if (textFromHandler != null) {
        fileText = textFromHandler;
        break;
      }
    }
    FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
    FileTemplate[] templates = templateManager.getAllTemplates();
    FileTemplate template = FileTemplateUtil.createTemplate(nameWithoutExtension, extension, fileText, templates);

    FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
    configurable.setProportion(0.6f);
    configurable.setTemplate(template, FileTemplateManagerImpl.getInstanceImpl(project).getDefaultTemplateDescription());
    SaveFileAsTemplateDialog dialog = new SaveFileAsTemplateDialog(project, configurable);
    if (dialog.showAndGet()) {
      templateManager.setTemplates(FileTemplateManager.DEFAULT_TEMPLATES_CATEGORY, Arrays.asList(ArrayUtil.append(templates, template)));
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.VIRTUAL_FILE) != null && e.getData(PlatformDataKeys.FILE_TEXT) != null);
  }
}
