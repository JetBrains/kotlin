// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.completion;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupActionProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesCheckboxTree;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesConfigurable;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.PlatformIcons;

public class PostfixTemplateLookupActionProvider implements LookupActionProvider {
  @Override
  public void fillActions(LookupElement element, final Lookup lookup, Consumer<LookupElementAction> consumer) {
    if (element instanceof PostfixTemplateLookupElement) {
      final PostfixTemplateLookupElement templateLookupElement = (PostfixTemplateLookupElement)element;
      final PostfixTemplate template = templateLookupElement.getPostfixTemplate();

      consumer.consume(new LookupElementAction(PlatformIcons.EDIT, "Edit postfix templates settings") {
        @Override
        public Result performLookupAction() {
          final Project project = lookup.getProject();
          ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;

            final PostfixTemplatesConfigurable configurable = new PostfixTemplatesConfigurable();
            ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
              PostfixTemplatesCheckboxTree templatesTree = configurable.getTemplatesTree();
              if (templatesTree != null) {
                templatesTree.selectTemplate(template, templateLookupElement.getProvider());
              }
            });
          });
          return Result.HIDE_LOOKUP;
        }
      });

      PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
      if (settings.isTemplateEnabled(template, templateLookupElement.getProvider())) {
        consumer.consume(new LookupElementAction(AllIcons.Actions.Delete, String.format("Disable '%s' template", template.getKey())) {
          @Override
          public Result performLookupAction() {
            ApplicationManager.getApplication().invokeLater(() -> settings.disableTemplate(template, templateLookupElement.getProvider()));
            return Result.HIDE_LOOKUP;
          }
        });
      }
    }
  }
}
