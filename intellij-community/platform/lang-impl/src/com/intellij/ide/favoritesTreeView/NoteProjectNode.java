/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class NoteProjectNode extends ProjectViewNodeWithChildrenList<NoteNode> {
  public NoteProjectNode(Project project, @NotNull NoteNode node, ViewSettings viewSettings) {
    super(project, node, viewSettings);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  public String toString() {
    return getValue().getText();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(getValue().getText());
    // todo define own color
    presentation.setForcedTextForeground(FileStatus.SWITCHED.getColor());
  }
}
