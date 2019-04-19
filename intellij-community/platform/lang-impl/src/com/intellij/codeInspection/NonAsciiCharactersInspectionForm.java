/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ReflectionUtil;
import gnu.trove.THashMap;

import javax.swing.*;
import java.util.Map;

class NonAsciiCharactersInspectionForm {
  private final NonAsciiCharactersInspection myInspection;
  private JBCheckBox myASCIIIdentifiers;
  private JBCheckBox myASCIIComments;
  private JBCheckBox myASCIIStringLiterals;
  private JBCheckBox myAlienIdentifiers;
  JPanel myPanel;
  private JBCheckBox myFilesContainingBOM;
  private final Map<JCheckBox, String> myBindings = new THashMap<>();

  NonAsciiCharactersInspectionForm(NonAsciiCharactersInspection inspection) {
    myInspection = inspection;
    bind(myASCIIIdentifiers, "CHECK_FOR_NOT_ASCII_IDENTIFIER_NAME");
    bind(myASCIIStringLiterals, "CHECK_FOR_NOT_ASCII_STRING_LITERAL");
    bind(myASCIIComments, "CHECK_FOR_NOT_ASCII_COMMENT");
    bind(myAlienIdentifiers, "CHECK_FOR_DIFFERENT_LANGUAGES_IN_IDENTIFIER_NAME");
    bind(myFilesContainingBOM, "CHECK_FOR_FILES_CONTAINING_BOM");

    reset();
  }

  private void bind(JCheckBox checkBox, String property) {
    myBindings.put(checkBox, property);
    reset(checkBox, property);
    checkBox.addChangeListener(__ -> {
      boolean selected = checkBox.isSelected();
      ReflectionUtil.setField(myInspection.getClass(), myInspection, boolean.class, property, selected);
    });
  }

  private void reset(JCheckBox checkBox, String property) {
    checkBox.setSelected(ReflectionUtil.getField(myInspection.getClass(), myInspection, boolean.class, property));
  }

  private void reset() {
    for (Map.Entry<JCheckBox, String> entry : myBindings.entrySet()) {
      JCheckBox checkBox = entry.getKey();
      String property = entry.getValue();
      reset(checkBox, property);
    }
  }
}
