/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.android.inspections.klint;

import com.android.tools.klint.detector.api.Issue;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class State {
  private final Module myModule;
  private final VirtualFile myMainFile;

  private final String myMainFileContent;
  private final List<ProblemData> myProblems = new ArrayList<ProblemData>();
  private final List<Issue> myIssues;

  private volatile boolean myDirty;

  State(@NotNull Module module,
        @NotNull VirtualFile mainFile,
        @NotNull String mainFileContent,
        @NotNull List<Issue> issues) {
    myModule = module;
    myMainFile = mainFile;
    myMainFileContent = mainFileContent;
    myIssues = issues;
  }

  @NotNull
  public VirtualFile getMainFile() {
    return myMainFile;
  }

  @NotNull
  public String getMainFileContent() {
    return myMainFileContent;
  }

  public void markDirty() {
    myDirty = true;
  }

  public boolean isDirty() {
    return myDirty;
  }

  @NotNull
  public Module getModule() {
    return myModule;
  }

  @NotNull
  public List<ProblemData> getProblems() {
    return myProblems;
  }

  @NotNull
  public List<Issue> getIssues() {
    return myIssues;
  }
}
