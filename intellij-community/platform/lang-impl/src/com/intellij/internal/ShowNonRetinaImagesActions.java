// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * @author Konstantin Bulenkov
 */
public class ShowNonRetinaImagesActions extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) return;
    class ImageInfo {
      boolean retina;
      boolean normal;
      boolean dark;
      boolean retina_dark;

    }
    HashMap<String, ImageInfo> info = new HashMap<>();
    final Collection<VirtualFile> images = FilenameIndex.getAllFilesByExt(project, "png", GlobalSearchScope.projectScope(project));
    for (VirtualFile image : images) {
      final String path = image.getPath();
      final String key = toKey(path);
      ImageInfo imageInfo = info.get(key);
      if (imageInfo == null) {
        imageInfo = new ImageInfo();
        info.put(key, imageInfo);
      }
      if (path.endsWith("@2x_dark.png")) {
        imageInfo.retina_dark = true;
      } else if (path.endsWith("_dark.png")) {
        imageInfo.dark = true;
      } else if (path.endsWith("@2x.png")) {
        imageInfo.retina = true;
      } else {
        imageInfo.normal = true;
      }
    }

    final ArrayList<String> retinaMissed = new ArrayList<>();
    for (String key : info.keySet()) {
      if (!info.get(key).retina && info.get(key).normal) {
        retinaMissed.add(key);
      }
    }
    retinaMissed.sort(String.CASE_INSENSITIVE_ORDER);

    new DialogWrapper(project) {
      {
        init();
      }

      @Nullable
      @Override
      protected JComponent createCenterPanel() {
        return new JBScrollPane(new JTextArea(StringUtil.join(retinaMissed, "\n")));
      }
    }.show();
  }

  private static String toKey(String path) {
    if (path.endsWith("@2x_dark.png")) {
      return path.substring(0, path.lastIndexOf("@2x_dark.png")) + ".png";
    }
    if (path.endsWith("_dark.png")) {
      return path.substring(0, path.lastIndexOf("_dark.png")) + ".png";
    }
    if (path.endsWith("@2x.png")) {
      return path.substring(0, path.lastIndexOf("@2x.png")) + ".png";
    }
    return path;
  }
}
