// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.memberPullUp;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoStorage;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoModel;
import com.intellij.refactoring.ui.AbstractMemberSelectionTable;
import com.intellij.refactoring.ui.MemberSelectionPanelBase;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Max Medvedev
 */
public abstract class PullUpDialogBase<Storage extends AbstractMemberInfoStorage<Member, Class, MemberInfo>,
                              MemberInfo extends MemberInfoBase<Member>,
                              Member extends PsiElement,
                              Class extends PsiElement> extends RefactoringDialog {
  protected MemberSelectionPanelBase<Member, MemberInfo, AbstractMemberSelectionTable<Member, MemberInfo>> myMemberSelectionPanel;
  protected MemberInfoModel<Member, MemberInfo> myMemberInfoModel;
  protected final Class myClass;
  protected final List<Class> mySuperClasses;
  protected final Storage myMemberInfoStorage;
  protected List<MemberInfo> myMemberInfos;
  private JComboBox myClassCombo;

  public PullUpDialogBase(Project project, Class aClass, List<Class> superClasses, Storage memberInfoStorage, String title) {
    super(project, true);
    myClass = aClass;
    mySuperClasses = superClasses;
    myMemberInfoStorage = memberInfoStorage;
    myMemberInfos = myMemberInfoStorage.getClassMemberInfos(aClass);

    setTitle(title);
  }

  @Nullable
  public Class getSuperClass() {
    if (myClassCombo != null) {
      return (Class) myClassCombo.getSelectedItem();
    }
    else {
      return null;
    }
  }

  public List<MemberInfo> getSelectedMemberInfos() {
    ArrayList<MemberInfo> list = new ArrayList<>(myMemberInfos.size());
    for (MemberInfo info : myMemberInfos) {
      if (info.isChecked() && myMemberInfoModel.isMemberEnabled(info)) {
        list.add(info);
      }
    }
    return list;
  }

  @Override
  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel();

    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = new Insets(4, 0, 4, 8);
    gbConstraints.weighty = 1;
    gbConstraints.weightx = 1;
    gbConstraints.gridy = 0;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.anchor = GridBagConstraints.WEST;
    final JLabel classComboLabel = new JLabel();
    panel.add(classComboLabel, gbConstraints);

    myClassCombo = new JComboBox(mySuperClasses.toArray());
    initClassCombo(myClassCombo);
    classComboLabel.setText(RefactoringBundle.message("pull.up.members.to", UsageViewUtil.getLongName(myClass)));
    classComboLabel.setLabelFor(myClassCombo);
    final Class preselection = getPreselection();
    int indexToSelect = 0;
    if (preselection != null) {
      indexToSelect = mySuperClasses.indexOf(preselection);
    }
    myClassCombo.setSelectedIndex(indexToSelect);
    myClassCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updateMemberInfo();
        }
      }
    });
    gbConstraints.gridy++;
    panel.add(myClassCombo, gbConstraints);

    return panel;
  }

  protected abstract void initClassCombo(JComboBox classCombo);

  protected abstract Class getPreselection();

  protected void updateMemberInfo() {
    final Class targetClass = (Class) myClassCombo.getSelectedItem();
    myMemberInfos = myMemberInfoStorage.getIntermediateMemberInfosList(targetClass);
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    myMemberSelectionPanel =
      new MemberSelectionPanelBase<>(RefactoringBundle.message("members.to.be.pulled.up"), createMemberSelectionTable(myMemberInfos));
    myMemberInfoModel = createMemberInfoModel();
    myMemberInfoModel.memberInfoChanged(new MemberInfoChange<>(myMemberInfos));
    myMemberSelectionPanel.getTable().setMemberInfoModel(myMemberInfoModel);
    myMemberSelectionPanel.getTable().addMemberInfoChangeListener(myMemberInfoModel);
    panel.add(myMemberSelectionPanel, BorderLayout.CENTER);

    addCustomElementsToCentralPanel(panel);
    updateMemberInfo();

    return panel;
  }

  protected void addCustomElementsToCentralPanel(JPanel panel) { }

  protected abstract AbstractMemberSelectionTable<Member,MemberInfo> createMemberSelectionTable(List<MemberInfo> infos);

  protected abstract MemberInfoModel<Member, MemberInfo> createMemberInfoModel();
}
