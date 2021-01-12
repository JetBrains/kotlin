// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.hint;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import javax.swing.*;
import java.util.List;

public final class ElementLocationUtil {

  private ElementLocationUtil() {
  }

  /**
   * @deprecated use {@link #renderElementLocation(PsiElement, Ref)}
   */
  @Deprecated
  public static void customizeElementLabel(final PsiElement element, final JLabel label) {
    Ref<Icon> ref = new Ref<>();
    label.setText(renderElementLocation(element, ref));
    label.setIcon(ref.get());
  }

  public static String renderElementLocation(final PsiElement element, final Ref<? super Icon> icon) {
    if (element != null) {
      PsiFile file = element.getContainingFile();
      VirtualFile vfile = file == null ? null : file.getVirtualFile();

      if (vfile == null) {
        icon.set(null);
        return "";
      }

      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
      final Module module = fileIndex.getModuleForFile(vfile);

      if (module != null) {
        icon.set(ModuleType.get(module).getIcon());
        return module.getName();
      }
      else {
        final List<OrderEntry> entries = fileIndex.getOrderEntriesForFile(vfile);

        OrderEntry entry = null;

        for (OrderEntry order : entries) {
          if (order instanceof LibraryOrderEntry || order instanceof JdkOrderEntry) {
            entry = order;
            break;
          }
        }

        if (entry != null) {
          icon.set(AllIcons.Nodes.PpLibFolder);
          return entry.getPresentableName();
        }
      }
    }
    icon.set(null);
    return "";
  }
}
