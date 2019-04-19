// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementSectionRulesValidator extends ArrangementMatchingRulesValidator {
  private final ArrangementSectionRuleManager mySectionRuleManager;

  public ArrangementSectionRulesValidator(ArrangementMatchingRulesModel model, ArrangementSectionRuleManager sectionRuleManager) {
    super(model);
    mySectionRuleManager = sectionRuleManager;
  }

  @Override
  @Nullable
  protected String validate(int index) {
    if (myRulesModel.getSize() < index) {
      return null;
    }

    if (mySectionRuleManager != null) {
      final ArrangementSectionRuleManager.ArrangementSectionRuleData data = extractSectionText(index);
      if (data != null) {
        return validateSectionRule(data, index);
      }
    }
    return super.validate(index);
  }

  @Nullable
  private String validateSectionRule(@NotNull ArrangementSectionRuleManager.ArrangementSectionRuleData data, int index) {
    int startSectionIndex = -1;
    final Set<String> sectionRules = ContainerUtil.newHashSet();
    for (int i = 0; i < index; i++) {
      final ArrangementSectionRuleManager.ArrangementSectionRuleData section = extractSectionText(i);
      if (section != null) {
        startSectionIndex = section.isSectionStart() ? i : -1;
        if (StringUtil.isNotEmpty(section.getText())) {
          sectionRules.add(section.getText());
        }
      }
    }
    if (StringUtil.isNotEmpty(data.getText()) && sectionRules.contains(data.getText())) {
      return ApplicationBundle.message("arrangement.settings.validation.duplicate.section.text");
    }

    if (!data.isSectionStart()) {
      if (startSectionIndex == -1) {
        return ApplicationBundle.message("arrangement.settings.validation.end.section.rule.without.start");
      }
      else if (startSectionIndex == index - 1) {
        return ApplicationBundle.message("arrangement.settings.validation.empty.section.rule");
      }
    }
    return null;
  }

  @Nullable
  private ArrangementSectionRuleManager.ArrangementSectionRuleData extractSectionText(int i) {
    Object element = myRulesModel.getElementAt(i);
    if (element instanceof StdArrangementMatchRule) {
      assert mySectionRuleManager != null;
      return mySectionRuleManager.getSectionRuleData((StdArrangementMatchRule)element);
    }
    return null;
  }
}
