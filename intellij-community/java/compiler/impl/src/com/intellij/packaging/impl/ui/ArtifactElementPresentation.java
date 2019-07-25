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
package com.intellij.packaging.impl.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.packaging.ui.TreeNodePresentation;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public class ArtifactElementPresentation extends TreeNodePresentation {
  private final ArtifactPointer myArtifactPointer;
  private final ArtifactEditorContext myContext;

  public ArtifactElementPresentation(ArtifactPointer artifactPointer, ArtifactEditorContext context) {
    myArtifactPointer = artifactPointer;
    myContext = context;
  }

  @Override
  public String getPresentableName() {
    return myArtifactPointer != null ? myArtifactPointer.getArtifactName(myContext.getArtifactModel()) : "<unknown>";
  }

  @Override
  public boolean canNavigateToSource() {
    return findArtifact() != null;
  }

  @Override
  public void navigateToSource() {
    final Artifact artifact = findArtifact();
    if (artifact != null) {
      myContext.selectArtifact(artifact);
    }
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    final Artifact artifact = findArtifact();
    Icon icon = artifact != null ? artifact.getArtifactType().getIcon() : AllIcons.Nodes.Artifact;
    presentationData.setIcon(icon);
    presentationData.addText(getPresentableName(), artifact != null ? mainAttributes : SimpleTextAttributes.ERROR_ATTRIBUTES);
  }

  @Nullable
  private Artifact findArtifact() {
    return myArtifactPointer != null ? myArtifactPointer.findArtifact(myContext.getArtifactModel()) : null;
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.ARTIFACT;
  }
}
