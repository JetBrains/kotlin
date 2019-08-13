/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import java.util.Collections;
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
    Collections.sort(retinaMissed, String.CASE_INSENSITIVE_ORDER);

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
