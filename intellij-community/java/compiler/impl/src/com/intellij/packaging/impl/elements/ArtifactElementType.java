// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.JavaCompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.elements.ComplexPackagingElementType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ArtifactElementType extends ComplexPackagingElementType<ArtifactPackagingElement> {
  public static final ArtifactElementType ARTIFACT_ELEMENT_TYPE = new ArtifactElementType();

  ArtifactElementType() {
    super("artifact", JavaCompilerBundle.message("element.type.name.artifact"));
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
    List<Artifact> artifacts = context.chooseArtifacts(getAvailableArtifacts(context, artifact, false), JavaCompilerBundle
      .message("dialog.title.choose.artifacts"));
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
    final Set<Artifact> result = ContainerUtil.set(context.getArtifactModel().getArtifacts());
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
    list.sort(ArtifactManager.ARTIFACT_COMPARATOR);
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
