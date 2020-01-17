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
package com.intellij.packaging.artifacts;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a reliable and efficient reference to (probably non-existing) artifact by its name. If you have a part of a project configuration
 * which refers to a artifact by name, you can store an instance returned by {@link ArtifactPointerManager#createPointer(String)} instead of
 * storing the artifact name. This allows you to get a Artifact instance via {@link #getArtifact()} which is more efficient than
 * {@link ArtifactManager#findArtifact(String)}, and {@link #getArtifactName()}  artifact name} encapsulated inside the instance will be properly
 * updated if the artifact it refers to is renamed.
 */
public interface ArtifactPointer {

  @NotNull
  String getArtifactName();

  @Nullable
  Artifact getArtifact();

  @NotNull
  String getArtifactName(@NotNull ArtifactModel artifactModel);

  @Nullable
  Artifact findArtifact(@NotNull ArtifactModel artifactModel);

}
