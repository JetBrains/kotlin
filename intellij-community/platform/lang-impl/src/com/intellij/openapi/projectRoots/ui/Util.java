// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.ui;

import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author MYakovlev
 */
public final class Util{

  public static VirtualFile showSpecifyJavadocUrlDialog(JComponent parent) {
    return showSpecifyJavadocUrlDialog(parent, "");
  }

  public static VirtualFile showSpecifyJavadocUrlDialog(JComponent parent, String initialValue){
    final String url = Messages.showInputDialog(parent, ProjectBundle.message("sdk.configure.javadoc.url.prompt"),
                                                ProjectBundle.message("sdk.configure.javadoc.url.title"), Messages.getQuestionIcon(), initialValue, new InputValidator() {
      @Override
      public boolean checkInput(String inputString) {
        return true;
      }
      @Override
      public boolean canClose(String inputString) {
        try {
          new URL(inputString);
          return true;
        }
        catch (MalformedURLException e1) {
          Messages.showErrorDialog(e1.getMessage(), ProjectBundle.message("sdk.configure.javadoc.url.title"));
        }
        return false;
      }
    });
    if (url == null) {
      return null;
    }
    return VirtualFileManager.getInstance().findFileByUrl(url);
  }


}
