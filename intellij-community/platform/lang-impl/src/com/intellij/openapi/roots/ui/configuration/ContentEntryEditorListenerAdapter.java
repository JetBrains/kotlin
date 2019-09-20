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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Zhuravlev
 * @author 2003
 */
public class ContentEntryEditorListenerAdapter implements ContentEntryEditor.ContentEntryEditorListener{
  @Override
  public void editingStarted(@NotNull ContentEntryEditor editor) {
  }

  @Override
  public void beforeEntryDeleted(@NotNull ContentEntryEditor editor) {
  }

  @Override
  public void sourceFolderAdded(@NotNull ContentEntryEditor editor, SourceFolder folder) {
  }

  @Override
  public void sourceFolderRemoved(@NotNull ContentEntryEditor editor, VirtualFile file) {
  }

  @Override
  public void folderExcluded(@NotNull ContentEntryEditor editor, VirtualFile file) {
  }

  @Override
  public void folderIncluded(@NotNull ContentEntryEditor editor, String fileUrl) {
  }

  @Override
  public void navigationRequested(@NotNull ContentEntryEditor editor, VirtualFile file) {
  }

  @Override
  public void sourceRootPropertiesChanged(@NotNull ContentEntryEditor editor, @NotNull SourceFolder folder) {
  }
}
