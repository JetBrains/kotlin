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
package com.intellij.packaging.elements;

import com.intellij.compiler.ant.GenerationOptions;
import com.intellij.compiler.ant.Generator;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public interface ArtifactAntGenerationContext {

  void runBeforeCurrentArtifact(Generator generator);

  void runBeforeBuild(Generator generator);

  void runAfterBuild(Generator generator);

  String createNewTempFileProperty(@NonNls String basePropertyName, @NonNls String fileName);

  String getModuleOutputPath(@NonNls String moduleName);

  String getModuleTestOutputPath(@NonNls String moduleName);

  String getSubstitutedPath(@NonNls String path);

  String getArtifactOutputProperty(@NotNull Artifact artifact);

  Project getProject();

  GenerationOptions getGenerationOptions();

  String getConfiguredArtifactOutputProperty(@NotNull Artifact artifact);
}
