// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.ide.DataManager;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.StandardTargetWeights;
import com.intellij.ide.impl.SelectInTargetPsiWrapper;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Anna Kozlova
 * @author Konstantin Bulenkov
 */
final class SelectInNavBarTarget extends SelectInTargetPsiWrapper implements DumbAware {
  public static final String NAV_BAR_ID = "NavBar";

  SelectInNavBarTarget(@NotNull Project project) {
    super(project);
  }

  @Override
  @NonNls
  public String getToolWindowId() {
    return NAV_BAR_ID;
  }

  @Override
  protected boolean canSelect(final PsiFileSystemItem file) {
    return UISettings.getInstance().getShowNavigationBar();
  }

  @Override
  protected void select(final Object selector, final VirtualFile virtualFile, final boolean requestFocus) {
    selectInNavBar(false);
  }

  @Override
  protected void select(final PsiElement element, boolean requestFocus) {
    selectInNavBar(false);
  }

  public static void selectInNavBar(boolean showPopup) {
    DataManager.getInstance().getDataContextFromFocus()
      .doWhenDone((Consumer<DataContext>)context -> {
        IdeFrame frame = IdeFrame.KEY.getData(context);
        if (frame instanceof IdeFrameEx) {
          final IdeRootPaneNorthExtension navBarExt = ((IdeFrameEx)frame).getNorthExtension(NavBarRootPaneExtension.NAV_BAR);
          if (navBarExt != null) {
            final JComponent c = navBarExt.getComponent();
            final NavBarPanel panel = (NavBarPanel)c.getClientProperty("NavBarPanel");
            panel.rebuildAndSelectItem((list) -> {
              if (UISettings.getInstance().getShowMembersInNavigationBar()) {
                int lastDirectory = ContainerUtil.lastIndexOf(list, (item) -> item.getObject() instanceof PsiDirectory || item.getObject() instanceof PsiDirectoryContainer);
                if (lastDirectory >= 0 && lastDirectory < list.size() - 1) return lastDirectory;
              }
              return list.size() - 1;
            }, showPopup);
          }
        }
      });
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.NAV_BAR_WEIGHT;
  }

  @Override
  public String getMinorViewId() {
    return null;
  }

  public String toString() {
    return SelectInManager.getNavBar();
  }
}
