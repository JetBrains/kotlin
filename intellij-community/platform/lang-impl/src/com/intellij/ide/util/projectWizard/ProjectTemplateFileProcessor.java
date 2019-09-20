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
package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * @author Dmitry Avdeev
 */
public abstract class ProjectTemplateFileProcessor {

  public static final ExtensionPointName<ProjectTemplateFileProcessor> EP_NAME = ExtensionPointName.create("com.intellij.projectTemplateFileProcessor");

  /** Return null if it can't be processed */
  @Nullable
  protected abstract String encodeFileText(String content, VirtualFile file, Project project) throws IOException;

  public static String encodeFile(String content, VirtualFile file, Project project) throws IOException {
    ProjectTemplateFileProcessor[] processors = EP_NAME.getExtensions();
    for (ProjectTemplateFileProcessor processor : processors) {
      String text = processor.encodeFileText(content, file, project);
      if (text != null) return text;
    }
    return content;
  }

  protected static String wrap(String param) {
    return "${" + param + "}";
  }
}
