package com.intellij.application.options;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.SchemeFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImportSchemeChooserDialog extends DialogWrapper {
  private JPanel contentPane;
  private JBList mySchemeList;
  private JTextField myTargetNameField;
  private JCheckBox myUseCurrentScheme;
  private String mySelectedName;
  private final static String UNNAMED_SCHEME_ITEM = "<" + ApplicationBundle.message("code.style.scheme.import.unnamed") + ">";
  private final List<String> myNames = new ArrayList<>();

  public ImportSchemeChooserDialog(@NotNull Project project,
                                   String[] schemeNames,
                                   final @Nullable String currScheme) {
    super(project, false);
    if (schemeNames.length > 0) {
      myNames.addAll(Arrays.asList(schemeNames));
    }
    else {
      myNames.add(UNNAMED_SCHEME_ITEM);
    }
    //noinspection unchecked
    mySchemeList.setModel(new DefaultListModel() {
      @Override
      public int getSize() {
        return myNames.size();
      }

      @Override
      public Object getElementAt(int index) {
        return myNames.get(index);
      }
    });
    mySchemeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mySchemeList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        int index = mySchemeList.getSelectedIndex();
        if (index >= 0) {
          mySelectedName = myNames.get(index);
          if (!myUseCurrentScheme.isSelected() && !UNNAMED_SCHEME_ITEM.equals(mySelectedName)) myTargetNameField.setText(mySelectedName);
        }
      }
    });
    myUseCurrentScheme.setEnabled(currScheme != null);
    myUseCurrentScheme.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myUseCurrentScheme.isSelected()) {
          myTargetNameField.setEnabled(false);
          if (currScheme != null) {
            myTargetNameField.setText(currScheme);
          }
        }
        else {
          myTargetNameField.setEnabled(true);
          if (mySelectedName != null) myTargetNameField.setText(mySelectedName);
        }
      }
    });
    mySchemeList.getSelectionModel().setSelectionInterval(0,0);
    init();
    setTitle(ApplicationBundle.message("title.import.scheme.chooser"));
  }

  public String getSelectedName() {
    return UNNAMED_SCHEME_ITEM.equals(mySelectedName) ? null : mySelectedName;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return contentPane;
  }

  public boolean isUseCurrentScheme() {
    return myUseCurrentScheme.isSelected();
  }

  @Nullable
  public String getTargetName() {
    String name = myTargetNameField.getText();
    return name != null && !name.trim().isEmpty() ? name : null;
  }

  public static Pair<String,CodeStyleScheme> selectOrCreateTargetScheme(@NotNull Project project,
                                                                        @NotNull CodeStyleScheme currentScheme,
                                                                        @NotNull SchemeFactory<? extends CodeStyleScheme> schemeFactory,
                                                                        String... schemeNames) {
    final ImportSchemeChooserDialog schemeChooserDialog =
      new ImportSchemeChooserDialog(project, schemeNames, !currentScheme.isDefault() ? currentScheme.getName() : null);
    if (schemeChooserDialog.showAndGet()) {
      return Pair.create(schemeChooserDialog.getSelectedName(),
        schemeChooserDialog.isUseCurrentScheme() && (!currentScheme.isDefault()) ? currentScheme :
             schemeFactory.createNewScheme(schemeChooserDialog.getTargetName()));
    }
    return null;
  }
}
