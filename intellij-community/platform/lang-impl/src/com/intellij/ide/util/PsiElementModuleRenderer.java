// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ui.UIUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Set;

public class PsiElementModuleRenderer extends DefaultListCellRenderer{
  private String myText;

  @Override
  public Component getListCellRendererComponent(
    JList list,
    Object value,
    int index,
    boolean isSelected,
    boolean cellHasFocus) {
    final Component listCellRendererComponent = super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
    customizeCellRenderer(value, isSelected);
    return listCellRendererComponent;
  }

  @Override
  public String getText() {
    return myText;
  }

  private void customizeCellRenderer(Object value, boolean selected) {
    myText = "";
    if (value instanceof PsiElement) {
      PsiElement element = (PsiElement)value;
      if (element.isValid()) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(element.getProject()).getFileIndex();
        VirtualFile vFile = PsiUtilCore.getVirtualFile(element);
        if (vFile != null && fileIndex.isInLibrary(vFile)){
          showLibraryLocation(fileIndex, vFile);
        }
        else {
          Module module = ModuleUtilCore.findModuleForPsiElement(element);
          if (module != null) {
            showProjectLocation(vFile, module, fileIndex);
          }
        }
      }
    }

    setText(myText);
    setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UIUtil.getListCellHPadding()));
    setHorizontalTextPosition(SwingConstants.LEFT);
    setHorizontalAlignment(SwingConstants.RIGHT); // align icon to the right
    setBackground(selected ? UIUtil.getListSelectionBackground() : UIUtil.getListBackground());
    setForeground(selected ? UIUtil.getListSelectionForeground() : UIUtil.getInactiveTextColor());
  }

  private void showProjectLocation(@Nullable VirtualFile vFile, @NotNull Module module, @NotNull ProjectFileIndex fileIndex) {
    boolean inTestSource = vFile != null && fileIndex.isInTestSourceContent(vFile);
    if (Registry.is("ide.show.folder.name.instead.of.module.name")) {
      String path = ModuleUtilCore.getModuleDirPath(module);
      myText = StringUtil.isEmpty(path) ? module.getName() : new File(path).getName();
    } else {
      myText = module.getName();
    }
    if (inTestSource) {
      setIcon(AllIcons.Modules.TestSourceFolder);
    }
    else {
      setIcon(ModuleType.get(module).getIcon());
    }
  }

  private void showLibraryLocation(ProjectFileIndex fileIndex, VirtualFile vFile) {
    setIcon(AllIcons.Nodes.PpLibFolder);
    for (OrderEntry order : fileIndex.getOrderEntriesForFile(vFile)) {
      if (order instanceof LibraryOrderEntry || order instanceof JdkOrderEntry) {
        myText = getPresentableName(order, vFile);
        break;
      }
    }

    if (StringUtil.isEmpty(myText) && Registry.is("index.run.configuration.jre")) {
      for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
        Set<VirtualFile> roots = StreamEx.of(sdk.getRootProvider().getFiles(OrderRootType.CLASSES))
                                         .append(sdk.getRootProvider().getFiles(OrderRootType.SOURCES))
                                         .toSet();
        if (VfsUtilCore.isUnder(vFile, roots)) {
          myText = "< " + sdk.getName() + " >";
          break;
        }
      }
    }

    myText = myText.substring(myText.lastIndexOf(File.separatorChar) + 1);
    VirtualFile jar = JarFileSystem.getInstance().getVirtualFileForJar(vFile);
    if (jar != null && !myText.equals(jar.getName())) {
      myText += " (" + jar.getName() + ")";
    }
  }

  protected String getPresentableName(final OrderEntry order, final VirtualFile vFile) {
    return order.getPresentableName();
  }
}
