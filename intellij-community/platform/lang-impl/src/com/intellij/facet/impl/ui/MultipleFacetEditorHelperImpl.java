// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.facet.impl.ui;

import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetEditorHelper;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.NotNullFunction;
import com.intellij.util.ui.ThreeStateCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class MultipleFacetEditorHelperImpl implements MultipleFacetEditorHelper {
  private static final Logger LOG = Logger.getInstance(MultipleFacetSettingsEditor.class);
  private final List<AbstractBinding> myBindings = new ArrayList<>();

  @Override
  public void bind(@NotNull ThreeStateCheckBox common, @NotNull FacetEditor[] editors, @NotNull NotNullFunction<? super FacetEditor, ? extends JCheckBox> fun) {
    List<JCheckBox> checkBoxesList = new ArrayList<>();
    for (FacetEditor editor : editors) {
      checkBoxesList.add(fun.fun(editor));
    }

    CheckBoxBinding checkBoxBinding = new CheckBoxBinding(common, checkBoxesList);
    myBindings.add(checkBoxBinding);
  }

  @Override
  public void bind(@NotNull JTextField common, @NotNull FacetEditor[] editors, @NotNull NotNullFunction<? super FacetEditor, ? extends JTextField> fun) {
    List<JTextField> componentsList = new ArrayList<>();
    for (FacetEditor editor : editors) {
      componentsList.add(fun.fun(editor));
    }

    TextFieldBinding binding = new TextFieldBinding(common, componentsList);
    myBindings.add(binding);
  }

  @Override
  public void bind(@NotNull JComboBox common, @NotNull FacetEditor[] editors, @NotNull NotNullFunction<? super FacetEditor, ? extends JComboBox> fun) {
    List<JComboBox> componentsList = new ArrayList<>();
    for (FacetEditor editor : editors) {
      componentsList.add(fun.fun(editor));
    }

    CombobBoxBinding binding = new CombobBoxBinding(common, componentsList);
    myBindings.add(binding);
  }

  @Override
  public void unbind() {
    for (AbstractBinding binding : myBindings) {
      binding.unbind();
    }
    myBindings.clear();
  }

  private static abstract class AbstractBinding {
    public abstract void unbind();
  }

  private static class CheckBoxBinding extends AbstractBinding implements ActionListener {
    private final ThreeStateCheckBox myCommon;
    private final List<? extends JCheckBox> myCheckBoxesList;
    private final List<Boolean> myInitialValues;

    CheckBoxBinding(final ThreeStateCheckBox common, final List<? extends JCheckBox> checkBoxesList) {
      LOG.assertTrue(!checkBoxesList.isEmpty());
      myCommon = common;
      myCheckBoxesList = checkBoxesList;

      Boolean initialValue = checkBoxesList.get(0).isSelected();
      myInitialValues = new ArrayList<>();
      for (JCheckBox checkBox : checkBoxesList) {
        boolean value = checkBox.isSelected();
        myInitialValues.add(value);
        if (initialValue != null && value != initialValue) {
          initialValue = null;
        }
      }
      if (initialValue != null) {
        common.setThirdStateEnabled(false);
        common.setSelected(initialValue);
      }
      else {
        common.setThirdStateEnabled(true);
        common.setState(ThreeStateCheckBox.State.DONT_CARE);
      }

      myCommon.addActionListener(this);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      ThreeStateCheckBox.State state = myCommon.getState();
      for (int i = 0; i < myCheckBoxesList.size(); i++) {
        boolean value = state == ThreeStateCheckBox.State.SELECTED ? true : state == ThreeStateCheckBox.State.NOT_SELECTED ? false : myInitialValues.get(i);
        JCheckBox checkBox = myCheckBoxesList.get(i);

        if (value != checkBox.isSelected()) {
          ButtonModel model = checkBox.getModel();
          model.setArmed(true);
          model.setPressed(true);
          model.setPressed(false);
          model.setArmed(false);
        }
      }
    }

    @Override
    public void unbind() {
      myCommon.removeActionListener(this);
    }
  }

  private static class TextFieldBinding extends AbstractBinding {
    private final JTextField myCommon;
    private final List<JTextField> myTextFields;
    private final List<String> myInitialValues;
    private final DocumentAdapter myListener;

    private TextFieldBinding(final JTextField common, final List<JTextField> textFields) {
      LOG.assertTrue(!textFields.isEmpty());
      myCommon = common;
      myTextFields = textFields;
      String initialValue = myTextFields.get(0).getText();
      myInitialValues = new ArrayList<>();
      for (JTextField field : myTextFields) {
        String value = field.getText();
        myInitialValues.add(value);
        if (initialValue != null && !initialValue.equals(value)) {
          initialValue = null;
        }
      }
      common.setText(initialValue != null ? initialValue : "");

      myListener = new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull final DocumentEvent e) {
          TextFieldBinding.this.textChanged();
        }
      };
      myCommon.getDocument().addDocumentListener(myListener);
    }

    protected void textChanged() {
      String value = myCommon.getText();
      for (int i = 0; i < myTextFields.size(); i++) {
        myTextFields.get(i).setText(value.length() == 0 ? myInitialValues.get(i) : value);
      }
    }

    @Override
    public void unbind() {
      myCommon.getDocument().removeDocumentListener(myListener);
    }
  }

  private static class CombobBoxBinding extends AbstractBinding implements ItemListener {
    private final JComboBox myCommon;
    private final List<? extends JComboBox> myComponentsList;
    private final List<Object> myInitialValues;

    CombobBoxBinding(final JComboBox common, final List<? extends JComboBox> componentsList) {
      LOG.assertTrue(!componentsList.isEmpty());
      myCommon = common;
      myComponentsList = componentsList;

      JComboBox first = componentsList.get(0);
      Object initialValue = first.getSelectedItem();

      myInitialValues = new ArrayList<>();
      for (JComboBox component : componentsList) {
        Object item = component.getSelectedItem();
        myInitialValues.add(item);
        if (initialValue != null && !initialValue.equals(item)) {
          initialValue = null;
        }
      }
      common.setSelectedItem(initialValue);

      common.addItemListener(this);
    }

    @Override
    public void unbind() {
      myCommon.removeItemListener(this);
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {
      Object item = myCommon.getSelectedItem();
      for (int i = 0; i < myComponentsList.size(); i++) {
        myComponentsList.get(i).setSelectedItem(item != null ? item : myInitialValues.get(i));
      }
    }
  }
}
