// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.filters.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.filters.HyperlinkInfoBase;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.ide.util.gotoByName.GotoFileCellRenderer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

class MultiPsiElementHyperlinkInfo extends HyperlinkInfoBase {
  private final Map<VirtualFile, SmartPsiElementPointer<?>> myMap;

  MultiPsiElementHyperlinkInfo(Collection<? extends PsiElement> elements) {
    SmartPointerManager manager = null;
    myMap = new LinkedHashMap<>();
    for (PsiElement element : elements) {
      if (manager == null) {
        manager = SmartPointerManager.getInstance(element.getProject());
      }
      myMap.put(element.getContainingFile().getVirtualFile(), manager.createSmartPsiElementPointer(element));
    }
  }

  @Override
  public void navigate(@NotNull Project project, @Nullable RelativePoint hyperlinkLocationPoint) {
    if (myMap.isEmpty()) return;
    if (myMap.size() == 1) {
      Map.Entry<VirtualFile, SmartPsiElementPointer<?>> entry = myMap.entrySet().iterator().next();
      navigateTo(project, entry.getKey(), entry.getValue());
      return;
    }
    JFrame frame = WindowManager.getInstance().getFrame(project);
    int width = frame != null ? frame.getSize().width : 200;
    JBPopup popup = JBPopupFactory.getInstance()
      .createPopupChooserBuilder(ContainerUtil.map(myMap.values(), ptr -> ptr.getContainingFile()))
      .setTitle(ExecutionBundle.message("popup.title.choose.target.file"))
      .setRenderer(new GotoFileCellRenderer(width))
      .setItemChosenCallback(selectedValue -> {
        VirtualFile file = selectedValue.getVirtualFile();
        navigateTo(project, file, myMap.get(file));
      })
      .createPopup();
    if (hyperlinkLocationPoint != null) {
      popup.show(hyperlinkLocationPoint);
    }
    else {
      popup.showInFocusCenter();
    }
  }

  private static void navigateTo(@NotNull Project project,
                                 VirtualFile file,
                                 SmartPsiElementPointer<?> pointer) {
    PsiElement element = pointer.getElement();
    int line = 0, column = 0;
    if (element != null) {
      Document document = element.getContainingFile().getViewProvider().getDocument();
      if (document != null) {
        int offset = element.getTextOffset();
        line = document.getLineNumber(offset);
        column = offset - document.getLineStartOffset(line);
      }
    }
    new OpenFileHyperlinkInfo(project, file, line, column).navigate(project);
  }
} 
  
