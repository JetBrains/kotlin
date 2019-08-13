/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.facet.impl.invalid;

import com.intellij.facet.impl.ui.MultipleFacetEditorHelperImpl;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.util.NotNullFunction;
import com.intellij.util.ui.ThreeStateCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author nik
 */
public class MultipleInvalidFacetEditor extends MultipleFacetSettingsEditor {
  private final MultipleFacetEditorHelperImpl myHelper;
  private JPanel myMainPanel;
  private ThreeStateCheckBox myIgnoreFacetsCheckBox;

  public MultipleInvalidFacetEditor(FacetEditor[] editors) {
    myHelper = new MultipleFacetEditorHelperImpl();
    myHelper.bind(myIgnoreFacetsCheckBox, editors, editor -> editor.getEditorTab(InvalidFacetEditor.class).getIgnoreCheckBox());
  }

  @Override
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  public void disposeUIResources() {
    myHelper.unbind();
  }
}
