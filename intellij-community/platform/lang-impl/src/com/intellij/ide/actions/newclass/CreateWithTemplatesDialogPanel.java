// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.newclass;

import com.intellij.ide.ui.newItemPopup.NewItemWithTemplatesPopupPanel;
import com.intellij.openapi.util.Trinity;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CreateWithTemplatesDialogPanel extends NewItemWithTemplatesPopupPanel<Trinity<String, Icon, String>> {

  public CreateWithTemplatesDialogPanel(@NotNull List<Trinity<String, Icon, String>> templates, @Nullable String selectedItem) {
    super(templates, LIST_RENDERER);
    myTemplatesList.addListSelectionListener(e -> {
      Trinity<String, Icon, String> selectedValue = myTemplatesList.getSelectedValue();
      if (selectedValue != null) {
        setTextFieldIcon(selectedValue.second);
      }
    });
    selectTemplate(selectedItem);
    setTemplatesListVisible(templates.size() > 1);
  }

  public JTextField getNameField() {
    return myTextField;
  }

  @NotNull
  public String getEnteredName() {
    return myTextField.getText().trim();
  }

  @NotNull
  public String getSelectedTemplate() {
    return myTemplatesList.getSelectedValue().third;
  }

  private void setTextFieldIcon(Icon icon) {
    myTextField.setExtensions(new TemplateIconExtension(icon));
    myTextField.repaint();
  }

  private void selectTemplate(@Nullable String selectedItem) {
    if (selectedItem == null) {
      myTemplatesList.setSelectedIndex(0);
      return;
    }

    ListModel<Trinity<String, Icon, String>> model = myTemplatesList.getModel();
    for (int i = 0; i < model.getSize(); i++) {
      String templateID = model.getElementAt(i).getThird();
      if (selectedItem.equals(templateID)) {
        myTemplatesList.setSelectedIndex(i);
        return;
      }
    }
  }

  private static final ListCellRenderer<Trinity<String, Icon, String>> LIST_RENDERER =
    new ListCellRenderer<Trinity<String, Icon, String>>() {

      private final ListCellRenderer<Trinity<String, Icon, String>> delegateRenderer =
        SimpleListCellRenderer.create((label, value, index) -> {
          if (value != null) {
            label.setText(value.first);
            label.setIcon(value.second);
          }
        });

      @Override
      public Component getListCellRendererComponent(JList<? extends Trinity<String, Icon, String>> list,
                                                    Trinity<String, Icon, String> value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus) {
        JComponent delegate = (JComponent) delegateRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        delegate.setBorder(JBUI.Borders.empty(JBUIScale.scale(3), JBUIScale.scale(1)));
        return delegate;
      }
    };

  private static class TemplateIconExtension implements ExtendableTextComponent.Extension {
    private final Icon icon;

    private TemplateIconExtension(Icon icon) {this.icon = icon;}

    @Override
    public Icon getIcon(boolean hovered) {
      return icon;
    }

    @Override
    public boolean isIconBeforeText() {
      return true;
    }
  }
}
