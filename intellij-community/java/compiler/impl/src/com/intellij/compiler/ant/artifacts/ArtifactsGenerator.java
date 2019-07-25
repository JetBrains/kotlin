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
package com.intellij.compiler.ant.artifacts;

import com.intellij.compiler.ant.*;
import com.intellij.compiler.ant.taskdefs.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.elements.ArtifactPackagingElement;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElement;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactsGenerator {
  @NonNls public static final String BUILD_ALL_ARTIFACTS_TARGET = "build.all.artifacts";
  @NonNls private static final String INIT_ARTIFACTS_TARGET = "init.artifacts";
  private final PackagingElementResolvingContext myResolvingContext;
  private final ArtifactAntGenerationContextImpl myContext;
  private final List<Artifact> myAllArtifacts;

  public ArtifactsGenerator(Project project, GenerationOptions genOptions) {
    myResolvingContext = ArtifactManager.getInstance(project).getResolvingContext();

    myAllArtifacts = new ArrayList<>(Arrays.asList(ArtifactManager.getInstance(project).getSortedArtifacts()));

    myContext = new ArtifactAntGenerationContextImpl(project, genOptions, myAllArtifacts);
  }

  public boolean hasArtifacts() {
    return !myAllArtifacts.isEmpty();
  }

  public List<Generator> generate() {
    final List<Generator> generators = new ArrayList<>();

    final Target initTarget = new Target(INIT_ARTIFACTS_TARGET, null, null, null);
    generators.add(initTarget);
    initTarget.add(new Property(ArtifactAntGenerationContextImpl.ARTIFACTS_TEMP_DIR_PROPERTY, BuildProperties.propertyRelativePath(BuildProperties.getProjectBaseDirProperty(), "__artifacts_temp")));

    for (Artifact artifact : myAllArtifacts) {
      if (!myContext.shouldBuildIntoTempDirectory(artifact)) {
        generators.add(new CleanArtifactTarget(artifact, myContext));
      }
      final String outputPath = artifact.getOutputPath();
      if (!StringUtil.isEmpty(outputPath)) {
        initTarget.add(new Property(myContext.getConfiguredArtifactOutputProperty(artifact), myContext.getSubstitutedPath(outputPath)));
      }
    }
    initTarget.add(new Mkdir(BuildProperties.propertyRef(ArtifactAntGenerationContextImpl.ARTIFACTS_TEMP_DIR_PROPERTY)));

    StringBuilder depends = new StringBuilder();
    for (Artifact artifact : myAllArtifacts) {
      Target target = createArtifactTarget(artifact);
      generators.add(target);

      if (!StringUtil.isEmpty(artifact.getOutputPath())) {
        if (depends.length() > 0) depends.append(", ");
        depends.append(myContext.getTargetName(artifact));
      }
    }

    for (Generator generator : myContext.getBeforeBuildGenerators()) {
      initTarget.add(generator);
    }

    initArtifacts(myResolvingContext.getProject(), initTarget);

    Target buildAllArtifacts = new Target(BUILD_ALL_ARTIFACTS_TARGET, depends.toString(), "Build all artifacts", null);
    for (Artifact artifact : myAllArtifacts) {
      final String artifactOutputPath = artifact.getOutputPath();
      if (!StringUtil.isEmpty(artifactOutputPath) && myContext.shouldBuildIntoTempDirectory(artifact)) {
        final String outputPath = BuildProperties.propertyRef(myContext.getConfiguredArtifactOutputProperty(artifact));
        buildAllArtifacts.add(new Mkdir(outputPath));
        final Copy copy = new Copy(outputPath);
        copy.add(new FileSet(BuildProperties.propertyRef(myContext.getArtifactOutputProperty(artifact))));
        buildAllArtifacts.add(copy);
      }
    }

    buildAllArtifacts.add(new Comment("Delete temporary files"), 1);
    for (Generator generator : myContext.getAfterBuildGenerators()) {
      buildAllArtifacts.add(generator);
    }
    buildAllArtifacts.add(new Delete(BuildProperties.propertyRef(ArtifactAntGenerationContextImpl.ARTIFACTS_TEMP_DIR_PROPERTY)));

    generators.add(buildAllArtifacts);
    return generators;
  }

  private Target createArtifactTarget(Artifact artifact) {
    final StringBuilder depends = new StringBuilder(INIT_ARTIFACTS_TARGET);

    ArtifactUtil.processRecursivelySkippingIncludedArtifacts(artifact, packagingElement -> {
      if (packagingElement instanceof ArtifactPackagingElement) {
        final Artifact included = ((ArtifactPackagingElement)packagingElement).findArtifact(myResolvingContext);
        if (included != null) {
          if (depends.length() > 0) depends.append(", ");
          depends.append(myContext.getTargetName(included));
        }
      }
      else if (packagingElement instanceof ModuleOutputPackagingElement) {
        final Module module = ((ModuleOutputPackagingElement)packagingElement).findModule(myResolvingContext);
        if (module != null) {
          if (depends.length() > 0) depends.append(", ");
          depends.append(BuildProperties.getCompileTargetName(module.getName()));
        }
      }
      return true;
    }, myResolvingContext);

    final Couple<String> xmlNs = getArtifactXmlNs(artifact.getArtifactType());
    final Target artifactTarget =
        new Target(myContext.getTargetName(artifact), depends.toString(), "Build '" + artifact.getName() + "' artifact", null,
                   Pair.getFirst(xmlNs), Pair.getSecond(xmlNs));

    if (myContext.shouldBuildIntoTempDirectory(artifact)) {
      final String outputDirectory = BuildProperties.propertyRelativePath(ArtifactAntGenerationContextImpl.ARTIFACTS_TEMP_DIR_PROPERTY,
                                                                          FileUtil.sanitizeFileName(artifact.getName()));
      artifactTarget.add(new Property(myContext.getArtifactOutputProperty(artifact), outputDirectory));
    }

    final String outputPath = BuildProperties.propertyRef(myContext.getArtifactOutputProperty(artifact));
    artifactTarget.add(new Mkdir(outputPath));
    generateTasksForArtifacts(artifact, artifactTarget, true);

    final DirectoryAntCopyInstructionCreator creator = new DirectoryAntCopyInstructionCreator(outputPath);

    List<Generator> copyInstructions = new ArrayList<>();
    if (needAntArtifactInstructions(artifact.getArtifactType())) {
      copyInstructions.addAll(artifact.getRootElement().computeAntInstructions(myResolvingContext, creator, myContext, artifact.getArtifactType()));
    }

    for (Generator generator : myContext.getAndClearBeforeCurrentArtifact()) {
      artifactTarget.add(generator);
    }
    for (Generator tag : copyInstructions) {
      artifactTarget.add(tag);
    }
    generateTasksForArtifacts(artifact, artifactTarget, false);
    return artifactTarget;
  }

  private void generateTasksForArtifacts(Artifact artifact, Target artifactTarget, final boolean preprocessing) {
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      extension.generateTasksForArtifact(artifact, preprocessing, myContext, artifactTarget);
    }
  }

  private void initArtifacts(Project project, CompositeGenerator generator) {
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      extension.initArtifacts(project, myContext.getGenerationOptions(), generator);
    }
  }

  private static Couple<String> getArtifactXmlNs(ArtifactType artifactType) {
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      final Couple<String> xmlNs = extension.getArtifactXmlNs(artifactType);
      if (xmlNs != null) return xmlNs;
    }
    return null;
  }

  private static boolean needAntArtifactInstructions(ArtifactType artifactType) {
    for (ChunkBuildExtension extension : ChunkBuildExtension.EP_NAME.getExtensions()) {
      if (!extension.needAntArtifactInstructions(artifactType)) return false;
    }
    return true;
  }

  public List<String> getCleanTargetNames() {
    final List<String> targets = new ArrayList<>();
    for (Artifact artifact : myAllArtifacts) {
      if (!myContext.shouldBuildIntoTempDirectory(artifact)) {
        targets.add(myContext.getCleanTargetName(artifact));
      }
    }
    return targets;
  }
}
