/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.arrangement.match.StdArrangementMatchRule;
import org.jetbrains.annotations.Nullable;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementMatchingRulesValidator {
  protected ArrangementMatchingRulesModel myRulesModel;


  public ArrangementMatchingRulesValidator(ArrangementMatchingRulesModel model) {
    myRulesModel = model;
  }

  @Nullable
  protected String validate(int index) {
    if (myRulesModel.getSize() < index) {
      return null;
    }

    final Object target = myRulesModel.getElementAt(index);
    if (target instanceof StdArrangementMatchRule) {
      for (int i = 0; i < index; i++) {
        final Object element = myRulesModel.getElementAt(i);
        if (element instanceof StdArrangementMatchRule && target.equals(element)) {
          return ApplicationBundle.message("arrangement.settings.validation.duplicate.matching.rule");
        }
      }
    }
    return null;
  }
}
