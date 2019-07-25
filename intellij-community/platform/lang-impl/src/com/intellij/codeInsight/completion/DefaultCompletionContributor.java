/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.documentation.actions.ShowQuickDocInfoAction;
import com.intellij.codeInsight.hint.actions.ShowImplementationsAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class DefaultCompletionContributor extends CompletionContributor {

  static void addDefaultAdvertisements(LookupImpl lookup, boolean includePsiFeatures) {
    Project project = lookup.getProject();
    if (CompletionUtil.shouldShowFeature(project, CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_DOT_ETC)) {
      lookup.addAdvertisement(LangBundle.message("completion.dot.etc.ad"), null);
    }
    if (!includePsiFeatures) return;
    
    if (CompletionUtil.shouldShowFeature(project, CodeCompletionFeatures.EDITING_COMPLETION_FINISH_BY_SMART_ENTER)) {
      final String shortcut = getActionShortcut(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM_COMPLETE_STATEMENT);
      if (StringUtil.isNotEmpty(shortcut)) {
        lookup.addAdvertisement(LangBundle.message("completion.smart.enter.ad", shortcut), null);
      }
    }

    if ((CompletionUtil.shouldShowFeature(project, ShowQuickDocInfoAction.CODEASSISTS_QUICKJAVADOC_FEATURE) ||
         CompletionUtil.shouldShowFeature(project, ShowQuickDocInfoAction.CODEASSISTS_QUICKJAVADOC_LOOKUP_FEATURE))) {
      final String shortcut = getActionShortcut(IdeActions.ACTION_QUICK_JAVADOC);
      if (StringUtil.isNotEmpty(shortcut)) {
        lookup.addAdvertisement(LangBundle.message("completion.quick.javadoc.ad", shortcut), null);
      }
    }

    if (CompletionUtil.shouldShowFeature(project, ShowImplementationsAction.CODEASSISTS_QUICKDEFINITION_FEATURE) ||
        CompletionUtil.shouldShowFeature(project, ShowImplementationsAction.CODEASSISTS_QUICKDEFINITION_LOOKUP_FEATURE)) {
      final String shortcut = getActionShortcut(IdeActions.ACTION_QUICK_IMPLEMENTATIONS);
      if (StringUtil.isNotEmpty(shortcut)) {
        lookup.addAdvertisement(LangBundle.message("completion.quick.implementations.ad", shortcut), null);
      }
    }
  }

  @Override
  public AutoCompletionDecision handleAutoCompletionPossibility(@NotNull AutoCompletionContext context) {
    final LookupElement[] items = context.getItems();
    if (items.length == 1) {
      final LookupElement item = items[0];
      if (!StringUtil.isEmpty(context.getLookup().itemPattern(item)) || context.getParameters().getCompletionType() == CompletionType.SMART) {
        return AutoCompletionDecision.insertItem(item);
      }
    }
    return null;
  }

}
