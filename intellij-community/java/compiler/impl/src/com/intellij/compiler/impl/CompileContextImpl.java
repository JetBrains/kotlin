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

/*
 * @author: Eugene Zhuravlev
 */
package com.intellij.compiler.impl;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.compiler.ProblemsView;
import com.intellij.compiler.progress.CompilerTask;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CompileContextImpl extends UserDataHolderBase implements CompileContextEx {
  public static final Key<CompileContext> CONTEXT_KEY = Key.create("jps_compile_context");
  private static final Logger LOG = Logger.getInstance("#com.intellij.compiler.impl.CompileContextImpl");
  private final Project myProject;
  private final CompilerTask myBuildSession;
  private final MessagesContainer myMessages;
  private final boolean myShouldUpdateProblemsView;
  private CompileScope myCompileScope;
  private final boolean myMake;
  private final boolean myIsRebuild;
  private final boolean myIsAnnotationProcessorsEnabled;
  private boolean myRebuildRequested;
  private String myRebuildReason;
  private final ProjectFileIndex myProjectFileIndex; // cached for performance reasons
  private final ProjectCompileScope myProjectCompileScope;
  private final long myStartCompilationStamp;
  private final UUID mySessionId = UUID.randomUUID();

  public CompileContextImpl(@NotNull Project project,
                            @NotNull CompilerTask compilerSession,
                            @NotNull CompileScope compileScope,
                            boolean isMake, boolean isRebuild) {
    myProject = project;
    myMessages = new MessagesContainer(project);
    myBuildSession = compilerSession;
    myCompileScope = compileScope;
    myMake = isMake;
    myIsRebuild = isRebuild;
    myStartCompilationStamp = System.currentTimeMillis();
    myProjectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    myProjectCompileScope = new ProjectCompileScope(myProject);
    myIsAnnotationProcessorsEnabled = CompilerConfiguration.getInstance(project).isAnnotationProcessorsEnabled();

    final Object sessionId = ExecutionManagerImpl.EXECUTION_SESSION_ID_KEY.get(compileScope);
    if (sessionId != null) {
      // in case compilation is started as a part of some execution session,
      // all compilation tasks should have the same sessionId in order for successive task not to clean messages
      // from previous compilation tasks run within this execution session
      compilerSession.setSessionId(sessionId);
    }
    final CompilerWorkspaceConfiguration workspaceConfig = CompilerWorkspaceConfiguration.getInstance(myProject);
    myShouldUpdateProblemsView = workspaceConfig.MAKE_PROJECT_ON_SAVE;
  }

  @NotNull
  public CompilerTask getBuildSession() {
    return myBuildSession;
  }

  public boolean shouldUpdateProblemsView() {
    return myShouldUpdateProblemsView;
  }

  public long getStartCompilationStamp() {
    return myStartCompilationStamp;
  }

  @Override
  @NotNull
  public Project getProject() {
    return myProject;
  }

  @NotNull
  @Override
  public CompilerMessage[] getMessages(@NotNull CompilerMessageCategory category) {
    return myMessages.getMessages(category).toArray(CompilerMessage.EMPTY_ARRAY);
  }

  @Override
  public void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum) {
    addMessage(category, message, url, lineNum, columnNum, null);
  }

  @Override
  public void addMessage(@NotNull CompilerMessageCategory category, String message, String url, int lineNum, int columnNum, Navigatable navigatable) {
    final CompilerMessage msg = myMessages.addMessage(category, message, url, lineNum, columnNum, navigatable);
    if (msg != null) {
      addToProblemsView(msg);
    }
  }

  @Override
  public void addMessage(CompilerMessage msg) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      LOG.info("addMessage: " + msg + " this=" + this);
    }
    if (myMessages.addMessage(msg)) {
      addToProblemsView(msg);
    }
  }

  private void addToProblemsView(CompilerMessage msg) {
    myBuildSession.addMessage(msg);
    if (myShouldUpdateProblemsView && msg.getCategory() == CompilerMessageCategory.ERROR) {
      ProblemsView.SERVICE.getInstance(myProject).addMessage(msg, mySessionId);
    }
  }

  @Override
  public int getMessageCount(CompilerMessageCategory category) {
    return myMessages.getMessageCount(category);
  }

  @Override
  public CompileScope getCompileScope() {
    return myCompileScope;
  }

  @Override
  public CompileScope getProjectCompileScope() {
    return myProjectCompileScope;
  }

  @Override
  public void requestRebuildNextTime(String message) {
    if (!myRebuildRequested) {
      myRebuildRequested = true;
      myRebuildReason = message;
    }
  }

  @Override
  public boolean isRebuildRequested() {
    return myRebuildRequested;
  }

  @Override
  @Nullable
  public String getRebuildReason() {
    return myRebuildReason;
  }

  @Override
  @NotNull
  public ProgressIndicator getProgressIndicator() {
    return myBuildSession.getIndicator();
  }

  @Override
  public Module getModuleByFile(@NotNull VirtualFile file) {
    final Module module = myProjectFileIndex.getModuleForFile(file);
    if (module != null) {
      LOG.assertTrue(!module.isDisposed());
      return module;
    }
    return null;
  }

  @Override
  public VirtualFile getModuleOutputDirectory(@NotNull Module module) {
    return CompilerPaths.getModuleOutputDirectory(module, false);
  }

  @Override
  public VirtualFile getModuleOutputDirectoryForTests(Module module) {
    return CompilerPaths.getModuleOutputDirectory(module, true);
  }

  @Override
  public boolean isMake() {
    return myMake;
  }

  @Override
  public boolean isAutomake() {
    return false;
  }

  @Override
  public boolean isRebuild() {
    return myIsRebuild;
  }

  @Override
  public boolean isAnnotationProcessorsEnabled() {
    return myIsAnnotationProcessorsEnabled;
  }

  @Override
  public void addScope(final CompileScope additionalScope) {
    myCompileScope = new CompositeScope(myCompileScope, additionalScope);
  }

  public UUID getSessionId() {
    return mySessionId;
  }
}
