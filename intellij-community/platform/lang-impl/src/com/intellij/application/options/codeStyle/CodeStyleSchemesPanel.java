/*
/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.application.options.codeStyle;

import com.intellij.application.options.schemes.AbstractSchemeActions;
import com.intellij.application.options.schemes.SchemesModel;
import com.intellij.application.options.schemes.SimpleSchemesPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.modifier.CodeStyleSettingsModifier;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CodeStyleSchemesPanel extends SimpleSchemesPanel<CodeStyleScheme> {
  
  private final CodeStyleSchemesModel myModel;
  
  private boolean myIsReset;

  public CodeStyleSchemesPanel(CodeStyleSchemesModel model, int vGap) {
    super(vGap);
    myModel = model;
  }
  CodeStyleSchemesPanel(CodeStyleSchemesModel model, @NotNull JComponent linkComponent) {
    super(DEFAULT_VGAP, linkComponent);
    myModel = model;
    showMessage(myModel.getOverridingStatus(), MessageType.INFO);
  }

  private void onCombo() {
    CodeStyleScheme selected = getSelectedScheme();
    if (selected != null) {
      myModel.selectScheme(selected, this);
    }
  }

  public void resetSchemesCombo() {
    myIsReset = true;
    try {
      List<CodeStyleScheme> schemes = new ArrayList<>(myModel.getAllSortedSchemes());
      resetSchemes(schemes);
      selectScheme(myModel.getSelectedScheme());
    }
    finally {
      myIsReset = false;
    }
  }

  public void onSelectedSchemeChanged() {
    myIsReset = true;
    try {
      selectScheme(myModel.getSelectedScheme());
    }
    finally {
      myIsReset = false;
    }
  }

  @NotNull
  @Override
  protected AbstractSchemeActions<CodeStyleScheme> createSchemeActions() {
    return
      new CodeStyleSchemesActions(this) {

        @Override
        protected void onSchemeChanged(@Nullable CodeStyleScheme scheme) {
          if (!myIsReset) {
            ApplicationManager.getApplication().invokeLater(() -> onCombo());
          }
        }

        @Override
        protected void renameScheme(@NotNull CodeStyleScheme scheme, @NotNull String newName) {
          CodeStyleSchemeImpl newScheme = new CodeStyleSchemeImpl(newName, false, scheme);
          myModel.addScheme(newScheme, false);
          myModel.removeScheme(scheme);
          myModel.selectScheme(newScheme, null);
        }
      };
  }

  @NotNull
  @Override
  public SchemesModel<CodeStyleScheme> getModel() {
    return myModel;
  }

  @Override
  protected boolean supportsProjectSchemes() {
    return true;
  }

  @Override
  protected boolean highlightNonDefaultSchemes() {
    return true;
  }

  @Override
  public boolean useBoldForNonRemovableSchemes() {
    return true;
  }

  public final void updateOverridingMessage() {
    showMessage(myModel.getOverridingStatus(), MessageType.INFO);
  }


}
