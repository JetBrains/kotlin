/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Scope;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class IntellijLintRequest extends LintRequest {
  @NonNull private final Project myProject;
  @NonNull private final List<Module> myModules;
  @NonNull private final IntellijLintClient mLintClient;
  @Nullable private final List<VirtualFile> myFileList;
  @Nullable private com.android.tools.lint.detector.api.Project myMainProject;
  private final boolean myIncremental;

  /**
   * Creates a new {@linkplain IntellijLintRequest}.
   * @param client the client
   * @param project the project where lint is run
   * @param fileList an optional list of specific files to check, normally null
   * @param modules the set of modules to be checked (or containing the files)
   * @param incremental true if this is an incremental (current editor) analysis
   */
  public IntellijLintRequest(@NonNull IntellijLintClient client,
                             @NonNull Project project,
                             @Nullable List<VirtualFile> fileList,
                             @NonNull List<Module> modules,
                             boolean incremental) {
    super(client, Collections.<File>emptyList());
    mLintClient = client;
    myProject = project;
    myModules = modules;
    myFileList = fileList;
    myIncremental = incremental;
  }

  @NonNull
  Project getProject() {
    return myProject;
  }

  @Nullable
  @Override
  public EnumSet<Scope> getScope() {
    if (mScope == null) {
      Collection<com.android.tools.lint.detector.api.Project> projects = getProjects();
      if (projects != null) {
        mScope = Scope.infer(projects);

        //noinspection ConstantConditions
        if (!IntellijLintProject.SUPPORT_CLASS_FILES && (mScope.contains(Scope.CLASS_FILE) || mScope.contains(Scope.ALL_CLASS_FILES)
                                                         || mScope.contains(Scope.JAVA_LIBRARIES))) {
          mScope = EnumSet.copyOf(mScope); // make mutable
          // Can't run class file based checks
          mScope.remove(Scope.CLASS_FILE);
          mScope.remove(Scope.ALL_CLASS_FILES);
          mScope.remove(Scope.JAVA_LIBRARIES);
        }
      }
    }

    return mScope;
  }

  @Nullable
  @Override
  public Collection<com.android.tools.lint.detector.api.Project> getProjects() {
    if (mProjects == null) {
      if (myIncremental && myFileList != null && myFileList.size() == 1 && myModules.size() == 1) {
        Pair<com.android.tools.lint.detector.api.Project,com.android.tools.lint.detector.api.Project> pair =
          IntellijLintProject.createForSingleFile(mLintClient, myFileList.get(0), myModules.get(0));
        mProjects = pair.first != null ? Collections.singletonList(pair.first)
                                       : Collections.<com.android.tools.lint.detector.api.Project>emptyList();
        myMainProject = pair.second;
      } else if (!myModules.isEmpty()) {
        // Make one project for each module, mark each one as a library,
        // and add projects for the gradle libraries and set error reporting to
        // false on those
        //mProjects = computeProjects()
        mProjects = IntellijLintProject.create(mLintClient, myFileList, myModules.toArray(new Module[myModules.size()]));
      } else {
        mProjects = super.getProjects();
      }
    }

    return mProjects;
  }

  @NonNull
  @Override
  public com.android.tools.lint.detector.api.Project getMainProject(@NonNull com.android.tools.lint.detector.api.Project project) {
    if (myMainProject != null) {
      return myMainProject;
    }
    return super.getMainProject(project);
  }
}
