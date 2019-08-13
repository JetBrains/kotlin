// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl.invalid;

import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.ex.MultiLineLabel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author nik
 */
public class InvalidFacetEditor extends FacetEditorTab {
  private final String myErrorMessage;
  private JPanel myMainPanel;
  private MultiLineLabel myDescriptionLabel;
  private JCheckBox myIgnoreCheckBox;
  private JLabel myIconLabel;
  private final InvalidFacetManager myInvalidFacetManager;
  private final InvalidFacet myFacet;

  public InvalidFacetEditor(FacetEditorContext context, String errorMessage) {
    myErrorMessage = errorMessage;
    myFacet = (InvalidFacet)context.getFacet();
    myInvalidFacetManager = InvalidFacetManager.getInstance(context.getProject());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "";
  }

  public JCheckBox getIgnoreCheckBox() {
    return myIgnoreCheckBox;
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    myIconLabel.setIcon(AllIcons.General.BalloonError);
    myDescriptionLabel.setText(myErrorMessage);
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    return myIgnoreCheckBox.isSelected() != myInvalidFacetManager.isIgnored(myFacet);
  }

  @Override
  public void reset() {
    myIgnoreCheckBox.setSelected(myInvalidFacetManager.isIgnored(myFacet));
  }

  @Override
  public void apply() {
    myInvalidFacetManager.setIgnored(myFacet, myIgnoreCheckBox.isSelected());
  }
}
