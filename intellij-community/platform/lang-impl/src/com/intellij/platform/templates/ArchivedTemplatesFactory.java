/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.platform.templates;

import com.intellij.icons.AllIcons;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class ArchivedTemplatesFactory extends ProjectTemplatesFactory {
  private final static Logger LOG = Logger.getInstance(ArchivedTemplatesFactory.class);

  static final String ZIP = ".zip";

  @NotNull
  private static URL getCustomTemplatesURL() {
    try {
      return new File(getCustomTemplatesPath()).toURI().toURL();
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  static String getCustomTemplatesPath() {
    return PathManager.getConfigPath() + "/projectTemplates";
  }

  @NotNull
  public static Path getTemplateFile(String name) {
    return Paths.get(getCustomTemplatesPath(), name + ".zip");
  }

  @NotNull
  @Override
  public String[] getGroups() {
    return new String[]{CUSTOM_GROUP};
  }

  @NotNull
  @Override
  public ProjectTemplate[] createTemplates(@Nullable String group, WizardContext context) {
    // myGroups contains only not-null keys
    if (!CUSTOM_GROUP.equals(group)) {
      return ProjectTemplate.EMPTY_ARRAY;
    }

    List<ProjectTemplate> templates = null;
    URL url = getCustomTemplatesURL();
    try {
      for (String child : UrlUtil.getChildrenRelativePaths(url)) {
        if (child.endsWith(ZIP)) {
          if (templates == null) {
            templates = new SmartList<>();
          }
          templates.add(new LocalArchivedTemplate(new URL(url.toExternalForm() + '/' + child), ClassLoader.getSystemClassLoader()));
        }
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
    return ContainerUtil.isEmpty(templates) ? ProjectTemplate.EMPTY_ARRAY : templates.toArray(ProjectTemplate.EMPTY_ARRAY);
  }

  @Override
  public int getGroupWeight(String group) {
    return CUSTOM_GROUP.equals(group) ? -2 : 0;
  }

  @Override
  public Icon getGroupIcon(String group) {
    return CUSTOM_GROUP.equals(group) ? AllIcons.General.User : super.getGroupIcon(group);
  }
}
