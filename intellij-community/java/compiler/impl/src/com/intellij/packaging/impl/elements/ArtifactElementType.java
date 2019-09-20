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
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.elements.ComplexPackagingElementType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
* @author nik
*/
public class ArtifactElementType extends ComplexPackagingElementType<ArtifactPackagingElement> {
  public static final ArtifactElementType ARTIFACT_ELEMENT_TYPE = new ArtifactElementType();

  ArtifactElementType() {
    super("artifact", CompilerBundle.message("element.type.name.artifact"));
  }

  @Override
  public Icon getCreateElementIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public boolean canCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact) {
    return !getAvailableArtifacts(context, artifact, false).isEmpty();
  }

  @Override
  @NotNull
  public List<? extends ArtifactPackagingElement> chooseAndCreate(@NotNull ArtifactEditorContext context, @NotNull Artifact artifact,
                                                                   @NotNull CompositePackagingElement<?> parent) {
    final Project project = context.getProject();
    List<Artifact> artifacts = context.chooseArtifacts(getAvailableArtifacts(context, artifact, false), CompilerBundle.message("dialog.title.choose.artifacts"));
    final List<ArtifactPackagingElement> elements = new ArrayList<>();
    for (Artifact selected : artifacts) {
      elements.add(new ArtifactPackagingElement(project, ArtifactPointerManager.getInstance(project).createPointer(selected, context.getArtifactModel())));
    }
    return elements;
  }

  @NotNull
  public static List<? extends Artifact> getAvailableArtifacts(@NotNull final ArtifactEditorContext context,
                                                               @NotNull final Artifact artifact,
                                                               final boolean notIncludedOnly) {
    final Set<Artifact> result = new HashSet<>(Arrays.asList(context.getArtifactModel().getArtifacts()));
    if (notIncludedOnly) {
      ArtifactUtil.processPackagingElements(artifact, ARTIFACT_ELEMENT_TYPE, artifactPackagingElement -> {
        result.remove(artifactPackagingElement.findArtifact(context));
        return true;
      }, context, true);
    }
    result.remove(artifact);
    final Iterator<Artifact> iterator = result.iterator();
    while (iterator.hasNext()) {
      Artifact another = iterator.next();
      final boolean notContainThis =
          ArtifactUtil.processPackagingElements(another, ARTIFACT_ELEMENT_TYPE,
                                                element -> !artifact.getName().equals(element.getArtifactName()), context, true);
      if (!notContainThis) {
        iterator.remove();
      }
    }
    final ArrayList<Artifact> list = new ArrayList<>(result);
    Collections.sort(list, ArtifactManager.ARTIFACT_COMPARATOR);
    return list;
  }

  @Override
  @NotNull
  public ArtifactPackagingElement createEmpty(@NotNull Project project) {
    return new ArtifactPackagingElement(project);
  }

  @Override
  public String getShowContentActionText() {
    return "Show Content of Included Artifacts";
  }
}
