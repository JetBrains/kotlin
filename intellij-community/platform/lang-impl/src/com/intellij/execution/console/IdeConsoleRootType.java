// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author gregsh
 */
public class IdeConsoleRootType extends ConsoleRootType {
  IdeConsoleRootType() {
    super("ide", ApplicationNamesInfo.getInstance().getProductName() + " Consoles");
  }

  @NotNull
  public static IdeConsoleRootType getInstance() {
    return findByClass(IdeConsoleRootType.class);
  }

  @Nullable
  @Override
  public Icon substituteIcon(@NotNull Project project, @NotNull VirtualFile file) {
    if (file.isDirectory()) return null;
    FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(file.getNameSequence());
    if (fileType == UnknownFileType.INSTANCE || fileType == PlainTextFileType.INSTANCE) {
      return AllIcons.Debugger.Console;
    }
    Icon icon = fileType.getIcon();
    Icon subscript = ((ScalableIcon)AllIcons.Debugger.Console).scale(.5f);
    LayeredIcon icons = new LayeredIcon(2);
    icons.setIcon(icon, 0);
    icons.setIcon(subscript, 1, 8, 8);
    return JBUI.scale(icons);
  }

  @Override
  public void fileOpened(@NotNull VirtualFile file, @NotNull FileEditorManager source) {
    RunIdeConsoleAction.configureConsole(file, source);
  }

}
