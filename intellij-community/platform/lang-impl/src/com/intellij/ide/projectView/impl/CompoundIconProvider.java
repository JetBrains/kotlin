// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IconProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.ui.IconManager;
import com.intellij.ui.icons.RowIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * This class is intended to combine all providers for batch usages.
 *
 * @author Sergey Malenkov
 */
public final class CompoundIconProvider extends IconProvider {
  private static final IconProvider INSTANCE = new CompoundIconProvider();
  private static final Logger LOG = Logger.getInstance(CompoundIconProvider.class);

  @Nullable
  @Override
  public Icon getIcon(@NotNull PsiElement element, int flags) {
    if (element.isValid()) {
      for (IconProvider provider : EXTENSION_POINT_NAME.getExtensions()) {
        try {
          Icon icon = provider.getIcon(element, flags);
          if (icon != null) {
            LOG.debug("icon found in ", provider);
            if (icon instanceof RowIcon) {
              RowIcon rowIcon = (RowIcon)icon;
              if (AllIcons.Nodes.Package.equals(rowIcon.getIcon(0)) && !isValidPackage(element)) {
                LOG.debug("fix row icon for invalid package: ", element);
                rowIcon.setIcon(AllIcons.Nodes.Folder, 0);
              }
            }
            else if (AllIcons.Nodes.Package.equals(icon) && !isValidPackage(element)) {
              LOG.debug("fix icon for invalid package: ", element);
              return AllIcons.Nodes.Folder;
            }
            return icon;
          }
        }
        catch (IndexNotReadyException exception) {
          throw new ProcessCanceledException(exception);
        }
        catch (ProcessCanceledException exception) {
          throw exception;
        }
        catch (Exception exception) {
          LOG.warn("unexpected error in " + provider, exception);
        }
      }
      if (element instanceof PsiDirectory) {
        LOG.debug("add default folder icon: ", element);
        return IconManager.getInstance().createLayeredIcon(element, AllIcons.Nodes.Folder, flags);
      }
    }
    return null;
  }

  @Nullable
  public static Icon findIcon(@Nullable PsiElement element, int flags) {
    return element == null ? null : INSTANCE.getIcon(element, flags);
  }

  private static boolean isValidPackage(@Nullable PsiElement element) {
    if (element instanceof PsiDirectory && element.isValid()) {
      PsiDirectoryFactory factory = PsiDirectoryFactory.getInstance(element.getProject());
      if (factory != null && factory.isPackage((PsiDirectory)element)) {
        return factory.isValidPackageName(factory.getQualifiedName((PsiDirectory)element, false));
      }
    }
    return false;
  }
}
