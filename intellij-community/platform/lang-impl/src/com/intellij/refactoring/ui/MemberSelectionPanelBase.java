// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.ui;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorFactory;

import javax.swing.*;
import java.awt.*;

/**
 * @author Max Medvedev
 */
public class MemberSelectionPanelBase<Member extends PsiElement,
                                      MemberInfo extends MemberInfoBase<Member>,
                                      Table extends AbstractMemberSelectionTable<Member, MemberInfo>> extends AbstractMemberSelectionPanel<Member, MemberInfo> {
  private final Table myTable;

  /**
   * @param title if title contains 'm' - it would look and feel as mnemonic
   */
  public MemberSelectionPanelBase(String title, Table table) {
    super();
    setLayout(new BorderLayout());

    myTable = table;
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);
    add(SeparatorFactory.createSeparator(title, myTable), BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);
  }

  @Override
  public Table getTable() {
    return myTable;
  }
}

