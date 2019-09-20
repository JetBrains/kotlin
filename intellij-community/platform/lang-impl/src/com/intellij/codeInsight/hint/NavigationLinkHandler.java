// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.highlighting.TooltipLinkHandler;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Handles tooltip links in format {@code #navigation/file_path:offset}.
 * On a click opens specified file in an editor and positions caret to the given offset.
 */
public class NavigationLinkHandler extends TooltipLinkHandler {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.hint.NavigationLinkHandler");

  @Override
  public boolean handleLink(@NotNull String refSuffix, @NotNull Editor editor) {
    int pos = refSuffix.lastIndexOf(':');
    if (pos <= 0 || pos == refSuffix.length() - 1) {
      LOG.info("Malformed suffix: " + refSuffix);
      return true;
    }

    String path = refSuffix.substring(0, pos);
    VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    if (vFile == null) {
      LOG.info("Unknown file: " + path);
      return true;
    }

    int offset;
    try {
      offset = Integer.parseInt(refSuffix.substring(pos + 1));
    }
    catch (NumberFormatException e) {
      LOG.info("Malformed suffix: " + refSuffix);
      return true;
    }

    Project project = editor.getProject();
    if (project == null) {
      LOG.info("No project");
      return true;
    }

    PsiNavigationSupport.getInstance().createNavigatable(project, vFile, offset).navigate(true);
    return true;
  }
}