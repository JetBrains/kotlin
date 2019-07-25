/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.usages.TextChunk;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsagePresentation;
import org.jetbrains.annotations.NotNull;

public class UsageProjectTreeNode extends ProjectViewNodeWithChildrenList<UsageInfo> {
  private final UsagePresentation myUsagePresentation;

  public UsageProjectTreeNode(Project project, @NotNull UsageInfo usage, ViewSettings viewSettings) {
    super(project, usage, viewSettings);
    final UsageInfo2UsageAdapter adapter = new UsageInfo2UsageAdapter(usage);
    myUsagePresentation = adapter.getPresentation();
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final UsageInfo info = getValue();
    if (info == null) return false;
    final PsiElement element = info.getElement();
    return element != null && file.equals(element.getContainingFile().getVirtualFile());
  }

  @Override
  public String toString() {
    return myUsagePresentation.getPlainText();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(myUsagePresentation.getIcon());
    presentation.setTooltip(myUsagePresentation.getTooltipText());
    final TextChunk[] text = myUsagePresentation.getText();
    updatePresentationWithTextChunks(presentation, text);
    presentation.setPresentableText(StringUtil.join(text, chunk -> chunk.getText(), ""));
  }

  public static void updatePresentationWithTextChunks(PresentationData presentation, TextChunk[] text) {
    for (TextChunk chunk : text) {
      presentation.addText(chunk.getText(), chunk.getSimpleAttributesIgnoreBackground());
    }
  }

  @Override
  public void navigate(boolean requestFocus) {
    UsageViewUtil.navigateTo(getValue(), requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return canNavigateToSource();
  }

  @Override
  public boolean canNavigateToSource() {
    return getValue().getElement().isValid();
  }
}
