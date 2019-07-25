// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.memberPushDown;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoModel;
import com.intellij.refactoring.ui.AbstractMemberSelectionPanel;
import com.intellij.refactoring.ui.DocCommentPanel;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.DocCommentPolicy;
import com.intellij.usageView.UsageViewUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractPushDownDialog<MemberInfo extends MemberInfoBase<Member>,
                                             Member extends PsiElement,
                                             Klass extends PsiElement> extends RefactoringDialog {
  private final List<MemberInfo> myMemberInfos;
  private final Klass myClass;
  private DocCommentPanel myJavaDocPanel;
  private MemberInfoModel<Member, MemberInfo> myMemberInfoModel;

  public AbstractPushDownDialog(Project project, MemberInfo[] memberInfos, Klass aClass) {
    super(project, true);
    myMemberInfos = Arrays.asList(memberInfos);
    myClass = aClass;

    setTitle(RefactoringBundle.message("push.members.down.title"));

    init();
  }

  public List<MemberInfo> getMemberInfos() {
    return myMemberInfos;
  }

  public Klass getSourceClass() {
    return myClass;
  }

  public ArrayList<MemberInfo> getSelectedMemberInfos() {
    return myMemberInfos.stream()
      .filter(info -> info.isChecked() && myMemberInfoModel.isMemberEnabled(info))
      .collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#com.intellij.refactoring.memberPushDown.PushDownDialog";
  }

  @Override
  protected JComponent createNorthPanel() {
    GridBagConstraints gbConstraints = new GridBagConstraints();

    JPanel panel = new JPanel(new GridBagLayout());

    gbConstraints.insets = new Insets(4, 0, 10, 8);
    gbConstraints.weighty = 1;
    gbConstraints.weightx = 1;
    gbConstraints.gridy = 0;
    gbConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gbConstraints.fill = GridBagConstraints.BOTH;
    gbConstraints.anchor = GridBagConstraints.WEST;
    panel.add(new JLabel(RefactoringBundle.message("push.members.from.0.down.label",
                                                   UsageViewUtil.getLongName(myClass))), gbConstraints);
    return panel;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    final AbstractMemberSelectionPanel<Member, MemberInfo> memberSelectionPanel = createMemberInfoPanel();
    panel.add(memberSelectionPanel, BorderLayout.CENTER);

    myMemberInfoModel = createMemberInfoModel();
    myMemberInfoModel.memberInfoChanged(new MemberInfoChange<>(myMemberInfos));
    memberSelectionPanel.getTable().setMemberInfoModel(myMemberInfoModel);
    memberSelectionPanel.getTable().addMemberInfoChangeListener(myMemberInfoModel);


    myJavaDocPanel = new DocCommentPanel(RefactoringBundle.message("push.down.javadoc.panel.title"));
    myJavaDocPanel.setPolicy(getDocCommentPolicy());
    panel.add(myJavaDocPanel, BorderLayout.EAST);
    return panel;
  }

  protected abstract MemberInfoModel<Member, MemberInfo> createMemberInfoModel();

  protected abstract AbstractMemberSelectionPanel<Member, MemberInfo> createMemberInfoPanel();

  protected abstract int getDocCommentPolicy();

  @Override
  protected void doAction() {
    if(!isOKActionEnabled()) return;

    savePreviewOption(isPreviewUsages());

    invokeRefactoring(new PushDownProcessor<>(myClass, getSelectedMemberInfos(), new DocCommentPolicy(myJavaDocPanel.getPolicy())));
  }

  protected abstract void savePreviewOption(boolean usages);
}
