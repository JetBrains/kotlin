// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.fileTemplates;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.Scheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Dmitry Avdeev
 */
public abstract class FileTemplatesScheme implements Scheme {

  public final static FileTemplatesScheme DEFAULT = new FileTemplatesScheme("Default") {
    @NotNull
    @Override
    public String getTemplatesDir() {
      return new File(PathManager.getConfigPath(), TEMPLATES_DIR).getPath();
    }

    @NotNull
    @Override
    public Project getProject() {
      return ProjectManager.getInstance().getDefaultProject();
    }
  };

  public static final String TEMPLATES_DIR = "fileTemplates";

  private final String myName;

  public FileTemplatesScheme(@NotNull String name) {
    myName = name;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public abstract String getTemplatesDir();

  @NotNull
  public abstract Project getProject();
}
