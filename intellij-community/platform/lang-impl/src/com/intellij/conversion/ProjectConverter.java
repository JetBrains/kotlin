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

package com.intellij.conversion;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Override some of 'create*Converter' methods to perform conversion. If none of these methods suits the needs,
 * override {@link #isConversionNeeded()}, {@link #getAdditionalAffectedFiles()} and one of '*processingFinished' methods
 *
 * @author nik
 */
public abstract class ProjectConverter {

  @Nullable
  public ConversionProcessor<ProjectSettings> createProjectFileConverter() {
    return null;
  }

  @Nullable
  public ConversionProcessor<ModuleSettings> createModuleFileConverter() {
    return null;
  }

  @Nullable
  public ConversionProcessor<WorkspaceSettings> createWorkspaceFileConverter() {
    return null;
  }

  @Nullable
  public ConversionProcessor<RunManagerSettings> createRunConfigurationsConverter() {
    return null;
  }

  @Nullable
  public ConversionProcessor<ProjectLibrariesSettings> createProjectLibrariesConverter() {
    return null;
  }

  @Nullable
  public ConversionProcessor<ArtifactsSettings> createArtifactsConverter() {
    return null;
  }

  /**
   * Override this method if conversion affects some configuration files not covered by provided {@link ConversionProcessor}s
   */
  public Collection<File> getAdditionalAffectedFiles() {
    return Collections.emptyList();
  }

  /**
   * @return files created during conversion process
   */
  public Collection<File> getCreatedFiles() {
    return Collections.emptyList();
  }

  /**
   * @return {@code true} if it's required to convert some files not covered by provided {@link ConversionProcessor}s
   */
  public boolean isConversionNeeded() {
    return false;
  }

  /**
   * Performs conversion of files not covered by provided {@link ConversionProcessor}s. Override this method if conversion should be
   * performed before {@link ConversionProcessor#process} for other converters is invoked
   */
  public void preProcessingFinished() throws CannotConvertException {
  }

  /**
   * Performs conversion of files not covered by provided {@link ConversionProcessor}s
   */
  public void processingFinished() throws CannotConvertException {
  }

  /**
   * Performs conversion of files not covered by provided {@link ConversionProcessor}s. Override this method if conversion should be
   * performed after {@link ConversionProcessor#process} for other converters is invoked
   */
  public void postProcessingFinished() throws CannotConvertException {
  }
}
