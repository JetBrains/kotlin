/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupActionProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.util.PlatformIcons;

/**
 * @author peter
 */
public class LiveTemplateLookupActionProvider implements LookupActionProvider {
  @Override
  public void fillActions(LookupElement element, final Lookup lookup, Consumer<LookupElementAction> consumer) {
    if (element instanceof LiveTemplateLookupElementImpl) {
      final TemplateImpl template = ((LiveTemplateLookupElementImpl)element).getTemplate();
      final TemplateImpl templateFromSettings = TemplateSettings.getInstance().getTemplate(template.getKey(), template.getGroupName());

      if (templateFromSettings != null) {
        consumer.consume(new LookupElementAction(PlatformIcons.EDIT, "Edit live template settings") {
          @Override
          public Result performLookupAction() {
            final Project project = lookup.getProject();
            ApplicationManager.getApplication().invokeLater(() -> {
              if (project.isDisposed()) return;

              final LiveTemplatesConfigurable configurable = new LiveTemplatesConfigurable();
              ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> configurable.getTemplateListPanel().editTemplate(template));
            });
            return Result.HIDE_LOOKUP;
          }
        });


        consumer.consume(new LookupElementAction(AllIcons.Actions.Delete, String.format("Disable '%s' template", template.getKey())) {
          @Override
          public Result performLookupAction() {
            ApplicationManager.getApplication().invokeLater(() -> templateFromSettings.setDeactivated(true));
            return Result.HIDE_LOOKUP;
          }
        });
      }
    }
  }
}
