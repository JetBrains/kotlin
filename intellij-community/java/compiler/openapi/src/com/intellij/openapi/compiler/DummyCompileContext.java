/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.compiler;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.DumbProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DummyCompileContext implements CompileContext {
  private final Project myProject;

  /**
   * @deprecated use {@link #create(Project)} instead
   */
  @Deprecated
  public DummyCompileContext() {
    this(ProjectManager.getInstance().getDefaultProject());
  }

  protected DummyCompileContext(Project project) {
    myProject = project;
  }

  /**
   * @deprecated use {@link #create(Project)} instead
   * @return
   */
  @Deprecated
  @NotNull
  public static DummyCompileContext getInstance() {
    return new DummyCompileContext(ProjectManager.getInstance().getDefaultProject());
  }

  @NotNull
  public static DummyCompileContext create(@NotNull Project project) {
    return new DummyCompileContext(project);
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) {
  }


  @Override
  public void addMessage(@NotNull CompilerMessageCategory category,
                         String message,
                         @Nullable String url,
                         int lineNum,
                         int columnNum,
                         Navigatable navigatable) {
  }

  @Override
  @NotNull
  public CompilerMessage[] getMessages(@NotNull CompilerMessageCategory category) {
    return CompilerMessage.EMPTY_ARRAY;
  }

  @Override
  public int getMessageCount(CompilerMessageCategory category) {
    return 0;
  }

  @Override
  @NotNull
  public ProgressIndicator getProgressIndicator() {
    return DumbProgressIndicator.INSTANCE;
  }

  @Override
  public CompileScope getCompileScope() {
    return null;
  }

  @Override
  public CompileScope getProjectCompileScope() {
    return null;
  }

  @Override
  public void requestRebuildNextTime(String message) {
  }

  @Override
  public boolean isRebuildRequested() {
    return false;
  }

  @Override
  @Nullable
  public String getRebuildReason() {
    return null;
  }

  @Override
  public Module getModuleByFile(@NotNull VirtualFile file) {
    return null;
  }

  @Override
  public boolean isAnnotationProcessorsEnabled() {
    return false;
  }

  @Override
  public VirtualFile getModuleOutputDirectory(@NotNull final Module module) {
    return ReadAction.compute(() -> CompilerModuleExtension.getInstance(module).getCompilerOutputPath());
  }

  @Override
  public VirtualFile getModuleOutputDirectoryForTests(Module module) {
    return null;
  }

  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, T value) {
  }

  @Override
  public boolean isMake() {
    return false; // stub implementation
  }

  @Override
  public boolean isAutomake() {
    return false;
  }

  @Override
  public boolean isRebuild() {
    return false;
  }
}
