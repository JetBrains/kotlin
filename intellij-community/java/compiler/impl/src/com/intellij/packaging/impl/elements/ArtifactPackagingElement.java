// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.elements;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.ui.ArtifactElementPresentation;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactPackagingElement extends ComplexPackagingElement<ArtifactPackagingElement.ArtifactPackagingElementState> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.packaging.impl.elements.ArtifactPackagingElement");
  private final Project myProject;
  private ArtifactPointer myArtifactPointer;
  @NonNls public static final String ARTIFACT_NAME_ATTRIBUTE = "artifact-name";

  public ArtifactPackagingElement(@NotNull Project project) {
    super(ArtifactElementType.ARTIFACT_ELEMENT_TYPE);
    myProject = project;
  }

  public ArtifactPackagingElement(@NotNull Project project, @NotNull ArtifactPointer artifactPointer) {
    this(project);
    myArtifactPointer = artifactPointer;
  }

  @Override
  public List<? extends PackagingElement<?>> getSubstitution(@NotNull PackagingElementResolvingContext context, @NotNull ArtifactType artifactType) {
    final Artifact artifact = findArtifact(context);
    if (artifact != null) {
      final ArtifactType type = artifact.getArtifactType();
      List<? extends PackagingElement<?>> substitution = type.getSubstitution(artifact, context, artifactType);
      if (substitution != null) {
        return substitution;
      }

      final List<PackagingElement<?>> elements = new ArrayList<>();
      final CompositePackagingElement<?> rootElement = artifact.getRootElement();
      if (rootElement instanceof ArtifactRootElement<?>) {
        elements.addAll(rootElement.getChildren());
      }
      else {
        elements.add(rootElement);
      }
      return elements;
    }
    return null;
  }

  @NotNull
  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext, @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {
    final Artifact artifact = findArtifact(resolvingContext);
    if (artifact != null) {
      if (artifact.getArtifactType().getSubstitution(artifact, resolvingContext, artifactType) != null) {
        return super.computeAntInstructions(resolvingContext, creator, generationContext, artifactType);
      }
      final String outputPath = BuildProperties.propertyRef(generationContext.getArtifactOutputProperty(artifact));
      return Collections.singletonList(creator.createDirectoryContentCopyInstruction(outputPath));
    }
    return Collections.emptyList();
  }

  @Override
  @NotNull
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(new ArtifactElementPresentation(myArtifactPointer, context));
  }

  @Override
  public ArtifactPackagingElementState getState() {
    final ArtifactPackagingElementState state = new ArtifactPackagingElementState();
    if (myArtifactPointer != null) {
      state.setArtifactName(myArtifactPointer.getArtifactName());
    }
    return state;
  }

  @Override
  public void loadState(@NotNull ArtifactPackagingElementState state) {
    final String name = state.getArtifactName();
    myArtifactPointer = name != null ? ArtifactPointerManager.getInstance(myProject).createPointer(name) : null;
  }

  @Override
  public String toString() {
    return "artifact:" + getArtifactName();
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof ArtifactPackagingElement && myArtifactPointer != null
           && myArtifactPointer.equals(((ArtifactPackagingElement)element).myArtifactPointer);
  }

  @Nullable
  public Artifact findArtifact(@NotNull PackagingElementResolvingContext context) {
    return myArtifactPointer != null ? myArtifactPointer.findArtifact(context.getArtifactModel()) : null;
  }

  @Nullable
  public String getArtifactName() {
    return myArtifactPointer != null ? myArtifactPointer.getArtifactName() : null;
  }


  public static class ArtifactPackagingElementState {
    private String myArtifactName;

    @Attribute(ARTIFACT_NAME_ATTRIBUTE)
    public String getArtifactName() {
      return myArtifactName;
    }

    public void setArtifactName(String artifactName) {
      myArtifactName = artifactName;
    }
  }
}
