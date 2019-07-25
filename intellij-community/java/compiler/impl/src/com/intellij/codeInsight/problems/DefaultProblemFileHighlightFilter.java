/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.problems;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;

/**
* @author yole
*/
public class DefaultProblemFileHighlightFilter implements Condition<VirtualFile> {
  private final Project myProject;

  public DefaultProblemFileHighlightFilter(Project project) {
    myProject = project;
  }

  @Override
  public boolean value(final VirtualFile file) {
    return FileIndexUtil.isJavaSourceFile(myProject, file)
      && !CompilerManager.getInstance(myProject).isExcludedFromCompilation(file);
  }
}
