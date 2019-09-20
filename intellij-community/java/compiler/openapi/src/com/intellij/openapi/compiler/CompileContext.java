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
package com.intellij.openapi.compiler;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface allowing access and modification of the data associated with the current compile session.
 */
public interface CompileContext extends UserDataHolder {
  /**
   * Allows to add a message to be shown in Compiler message view.
   * If correct url, line and column numbers are supplied, the navigation to the specified file is available from the view.
   *
   * @param category  the category of a message (information, error, warning).
   * @param message   the text of the message.
   * @param url       a url to the file to which the message applies, null if not available.
   * @param lineNum   a line number, -1 if not available.
   * @param columnNum a column number, -1 if not available.
   */
  void addMessage(@NotNull CompilerMessageCategory category, String message, @Nullable String url, int lineNum, int columnNum);

  /**
   * Allows to add a message to be shown in Compiler message view, with a specified Navigatable
   * that is used to navigate to the error location.
   *
   * @param category    the category of a message (information, error, warning).
   * @param message     the text of the message.
   * @param url         a url to the file to which the message applies, null if not available.
   * @param lineNum     a line number, -1 if not available.
   * @param columnNum   a column number, -1 if not available.
   * @param navigatable the navigatable pointing to the error location.
   */
  void addMessage(@NotNull CompilerMessageCategory category, String message, @Nullable String url, int lineNum, int columnNum,
                  Navigatable navigatable);

  /**
   * Returns all messages of the specified category added during the current compile session.
   *
   * @param category the category for which messages are requested.
   * @return all compiler messages of the specified category
   */
  @NotNull
  CompilerMessage[] getMessages(@NotNull CompilerMessageCategory category);

  /**
   * Returns the count of messages of the specified category added during the current compile session.
   *
   * @param category the category for which messages are requested.
   * @return the number of messages of the specified category
   */
  int getMessageCount(@Nullable CompilerMessageCategory category);

  /**
   * Returns the progress indicator of the compilation process.
   *
   * @return the progress indicator instance.
   */
  @NotNull
  ProgressIndicator getProgressIndicator();

  /**
   * Returns the current compile scope.
   *
   * @return current compile scope
   */
  CompileScope getCompileScope();

  /**
   * Returns the compile scope which would be used if the entire project was rebuilt.
   * {@link #getCompileScope()} may return the scope, that is more narrow than ProjectCompileScope.
   *
   * @return project-wide compile scope.
   */
  CompileScope getProjectCompileScope();

  /**
   * A compiler may call this method in order to request complete project rebuild.
   * This may be necessary, for example, when compiler caches are corrupted.
   */
  void requestRebuildNextTime(String message);

  boolean isRebuildRequested();

  @Nullable
  String getRebuildReason();

  /**
   * Returns the module to which the specified file belongs. This method is aware of the file->module mapping
   * for generated files.
   *
   * @param file the file to check.
   * @return the module to which the file belongs
   */
  Module getModuleByFile(@NotNull VirtualFile file);

  /**
   * Returns the output directory for the specified module.
   *
   * @param module the module to check.
   * @return the output directory for the module specified, null if corresponding VirtualFile is not valid or directory not specified
   */
  @Nullable
  VirtualFile getModuleOutputDirectory(@NotNull Module module);

  /**
   * Returns the test output directory for the specified module.
   *
   * @param module the module to check.
   * @return the tests output directory the module specified, null if corresponding VirtualFile is not valid. If in Paths settings
   *         output directory for tests is not configured explicitly, but the output path is present, the output path will be returned.
   */
  @Nullable
  VirtualFile getModuleOutputDirectoryForTests(Module module);

  /**
   * Checks if the compilation is incremental, i.e. triggered by one of "Make" actions.
   *
   * @return true if compilation is incremental. 
   */
  boolean isMake();

  boolean isAutomake();

  boolean isRebuild();

  @NotNull
  Project getProject();

  boolean isAnnotationProcessorsEnabled();
}
