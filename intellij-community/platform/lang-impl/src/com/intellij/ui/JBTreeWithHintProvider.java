/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.ide.DataManager;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.psi.PsiElement;
import com.intellij.ui.popup.HintUpdateSupply;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

/**
 * @author Konstantin Bulenkov
 * @deprecated use HintUpdateSupply directly
 * @see HintUpdateSupply
 */
@Deprecated
public class JBTreeWithHintProvider extends DnDAwareTree {
  {
    HintUpdateSupply.installHintUpdateSupply(this, o -> getPsiElementForHint(o));
  }

  public JBTreeWithHintProvider() {
  }

  public JBTreeWithHintProvider(TreeModel treemodel) {
    super(treemodel);
  }

  public JBTreeWithHintProvider(TreeNode root) {
    super(root);
  }

  @Nullable
  protected PsiElement getPsiElementForHint(final Object selectedValue) {
    return CommonDataKeys.PSI_ELEMENT.getData(DataManager.getInstance().getDataContext(this));
  }
}
