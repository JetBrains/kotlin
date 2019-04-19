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

package com.intellij.compiler.ant;

import com.intellij.ExtensionPoints;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.ArtifactAntGenerationContext;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ChunkBuildExtension {
  public static final ExtensionPointName<ChunkBuildExtension> EP_NAME = ExtensionPointName.create(ExtensionPoints.ANT_BUILD_GEN);

  @NotNull
  @NonNls
  public abstract String[] getTargets(final ModuleChunk chunk);

  public abstract void process(Project project, ModuleChunk chunk, GenerationOptions genOptions, CompositeGenerator generator);

  public void generateProjectTargets(Project project, GenerationOptions genOptions, CompositeGenerator generator) {
  }

  public void generateProperties(final PropertyFileGenerator generator, final Project project, final GenerationOptions options) {
  }

  public void generateTasksForArtifact(Artifact artifact, boolean preprocessing, ArtifactAntGenerationContext context,
                                       CompositeGenerator generator) {
  }

  @Nullable
  public Couple<String> getArtifactXmlNs(ArtifactType artifactType) {
    return null;
  } 

  public boolean needAntArtifactInstructions(ArtifactType type) {
    return true;
  }
  
  public void initArtifacts(Project project, GenerationOptions genOptions, CompositeGenerator generator) {}
  
  public List<String> getCleanTargetNames(Project project, GenerationOptions genOptions) {
    return Collections.emptyList();
  }

  public static String[] getAllTargets(ModuleChunk chunk) {
    List<String> allTargets = new ArrayList<>();
    final ChunkBuildExtension[] extensions = Extensions.getRootArea().getExtensionPoint(EP_NAME).getExtensions();
    for (ChunkBuildExtension extension : extensions) {
      ContainerUtil.addAll(allTargets, extension.getTargets(chunk));
    }
    if (allTargets.isEmpty()) {
      allTargets.add(BuildProperties.getCompileTargetName(chunk.getName()));
    }
    return ArrayUtil.toStringArray(allTargets);
  }

  public static void process(CompositeGenerator generator, ModuleChunk chunk, GenerationOptions genOptions) {
    final Project project = chunk.getProject();
    final ChunkBuildExtension[] extensions = Extensions.getRootArea().getExtensionPoint(EP_NAME).getExtensions();
    for (ChunkBuildExtension extension : extensions) {
      extension.process(project, chunk, genOptions, generator);
    }
  }

  public static void generateAllProperties(final PropertyFileGenerator propertyFileGenerator,
                                           final Project project,
                                           final GenerationOptions genOptions) {
    ChunkBuildExtension[] extensions = Extensions.getRootArea().getExtensionPoint(EP_NAME).getExtensions();
    for (ChunkBuildExtension extension : extensions) {
      extension.generateProperties(propertyFileGenerator, project, genOptions);
    }
  }
}
