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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.usages.TextChunk;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InvalidUsageNoteProjectNode extends ProjectViewNodeWithChildrenList<InvalidUsageNoteNode> {
  public InvalidUsageNoteProjectNode(Project project, @NotNull InvalidUsageNoteNode node, ViewSettings viewSettings) {
    super(project, node, viewSettings);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    final List<TextChunk> text = getValue().getText();
    if (!text.isEmpty()) {
      UsageProjectTreeNode.updatePresentationWithTextChunks(presentation, text.toArray(TextChunk.EMPTY_ARRAY));
    }
  }
}
