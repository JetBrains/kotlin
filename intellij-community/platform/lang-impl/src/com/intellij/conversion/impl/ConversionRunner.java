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

package com.intellij.conversion.impl;

import com.intellij.conversion.*;
import com.intellij.openapi.components.StorageScheme;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class ConversionRunner {
  private final ConverterProvider myProvider;
  private final ConversionContextImpl myContext;
  private final ConversionProcessor<ModuleSettings> myModuleFileConverter;
  private final ConversionProcessor<ProjectSettings> myProjectFileConverter;
  private final ConversionProcessor<WorkspaceSettings> myWorkspaceConverter;
  private boolean myProcessProjectFile;
  private boolean myProcessWorkspaceFile;
  private boolean myProcessRunConfigurations;
  private boolean myProcessProjectLibraries;
  private boolean myArtifacts;
  private final List<File> myModulesFilesToProcess = new ArrayList<>();
  private final ProjectConverter myConverter;
  private final ConversionProcessor<RunManagerSettings> myRunConfigurationsConverter;
  private final ConversionProcessor<ProjectLibrariesSettings> myProjectLibrariesConverter;
  private final ConversionProcessor<ArtifactsSettings> myArtifactsConverter;

  public ConversionRunner(ConverterProvider provider, ConversionContextImpl context) {
    myProvider = provider;
    myContext = context;
    myConverter = provider.createConverter(context);
    myModuleFileConverter = myConverter.createModuleFileConverter();
    myProjectFileConverter = myConverter.createProjectFileConverter();
    myWorkspaceConverter = myConverter.createWorkspaceFileConverter();
    myRunConfigurationsConverter = myConverter.createRunConfigurationsConverter();
    myProjectLibrariesConverter = myConverter.createProjectLibrariesConverter();
    myArtifactsConverter = myConverter.createArtifactsConverter();
  }

  public boolean isConversionNeeded() throws CannotConvertException {
    if (myContext.isConversionAlreadyPerformed(myProvider)) return false;
    
    myProcessProjectFile = myContext.getStorageScheme() == StorageScheme.DEFAULT && myProjectFileConverter != null
                           && myProjectFileConverter.isConversionNeeded(myContext.getProjectSettings());

    myProcessWorkspaceFile = myWorkspaceConverter != null && myContext.getWorkspaceFile().exists()
                             && myWorkspaceConverter.isConversionNeeded(myContext.getWorkspaceSettings());

    myModulesFilesToProcess.clear();
    if (myModuleFileConverter != null) {
      for (File moduleFile : myContext.getModuleFiles()) {
        if (moduleFile.exists() && myModuleFileConverter.isConversionNeeded(myContext.getModuleSettings(moduleFile))) {
          myModulesFilesToProcess.add(moduleFile);
        }
      }
    }

    myProcessRunConfigurations = myRunConfigurationsConverter != null
                                 && myRunConfigurationsConverter.isConversionNeeded(myContext.getRunManagerSettings());

    myProcessProjectLibraries = myProjectLibrariesConverter != null
                                 && myProjectLibrariesConverter.isConversionNeeded(myContext.getProjectLibrariesSettings());

    myArtifacts = myArtifactsConverter != null
                  && myArtifactsConverter.isConversionNeeded(myContext.getArtifactsSettings());

    return myProcessProjectFile ||
           myProcessWorkspaceFile ||
           myProcessRunConfigurations ||
           myProcessProjectLibraries ||
           !myModulesFilesToProcess.isEmpty() ||
           myConverter.isConversionNeeded();
  }

  public boolean isModuleConversionNeeded(File moduleFile) throws CannotConvertException {
    return myModuleFileConverter != null && myModuleFileConverter.isConversionNeeded(myContext.getModuleSettings(moduleFile));
  }

  public Collection<File> getCreatedFiles() {
    return myConverter.getCreatedFiles();
  }

  public Set<File> getAffectedFiles() {
    Set<File> affectedFiles = new HashSet<>();
    if (myProcessProjectFile) {
      affectedFiles.add(myContext.getProjectFile());
    }
    if (myProcessWorkspaceFile) {
      affectedFiles.add(myContext.getWorkspaceFile());
    }
    affectedFiles.addAll(myModulesFilesToProcess);

    try {
      if (myProcessRunConfigurations) {
        affectedFiles.addAll(myContext.getRunManagerSettings().getAffectedFiles());
      }
      if (myProcessProjectLibraries) {
        affectedFiles.addAll(myContext.getProjectLibrariesSettings().getAffectedFiles());
      }
      if (myArtifacts) {
        affectedFiles.addAll(myContext.getArtifactsSettings().getAffectedFiles());
      }
    }
    catch (CannotConvertException ignored) {
    }
    if (!myProvider.canDetermineIfConversionAlreadyPerformedByProjectFiles()) {
      final ComponentManagerSettings settings = myContext.getProjectFileVersionSettings();
      if (settings != null) {
        affectedFiles.add(settings.getFile());
      }
    }
    
    affectedFiles.addAll(myConverter.getAdditionalAffectedFiles());
    return affectedFiles;
  }

  public void preProcess() throws CannotConvertException {
    if (myProcessProjectFile) {
      myProjectFileConverter.preProcess(myContext.getProjectSettings());
    }

    if (myProcessWorkspaceFile) {
      myWorkspaceConverter.preProcess(myContext.getWorkspaceSettings());
    }

    for (File moduleFile : myModulesFilesToProcess) {
      myModuleFileConverter.preProcess(myContext.getModuleSettings(moduleFile));
    }

    if (myProcessRunConfigurations) {
      myRunConfigurationsConverter.preProcess(myContext.getRunManagerSettings());
    }

    if (myProcessProjectLibraries) {
      myProjectLibrariesConverter.preProcess(myContext.getProjectLibrariesSettings());
    }

    if (myArtifacts) {
      myArtifactsConverter.preProcess(myContext.getArtifactsSettings());
    }
    myConverter.preProcessingFinished();
  }

  public void process() throws CannotConvertException {
    if (myProcessProjectFile) {
      myProjectFileConverter.process(myContext.getProjectSettings());
    }

    if (myProcessWorkspaceFile) {
      myWorkspaceConverter.process(myContext.getWorkspaceSettings());
    }

    for (File moduleFile : myModulesFilesToProcess) {
      myModuleFileConverter.process(myContext.getModuleSettings(moduleFile));
    }

    if (myProcessRunConfigurations) {
      myRunConfigurationsConverter.process(myContext.getRunManagerSettings());
    }

    if (myProcessProjectLibraries) {
      myProjectLibrariesConverter.process(myContext.getProjectLibrariesSettings());
    }

    if (myArtifacts) {
      myArtifactsConverter.process(myContext.getArtifactsSettings());
    }
    myConverter.processingFinished();
  }

  public void postProcess() throws CannotConvertException {
    if (myProcessProjectFile) {
      myProjectFileConverter.postProcess(myContext.getProjectSettings());
    }

    if (myProcessWorkspaceFile) {
      myWorkspaceConverter.postProcess(myContext.getWorkspaceSettings());
    }

    for (File moduleFile : myModulesFilesToProcess) {
      myModuleFileConverter.postProcess(myContext.getModuleSettings(moduleFile));
    }

    if (myProcessRunConfigurations) {
      myRunConfigurationsConverter.postProcess(myContext.getRunManagerSettings());
    }

    if (myProcessProjectLibraries) {
      myProjectLibrariesConverter.postProcess(myContext.getProjectLibrariesSettings());
    }

    if (myArtifacts) {
      myArtifactsConverter.postProcess(myContext.getArtifactsSettings());
    }
    myConverter.postProcessingFinished();
  }

  public ConverterProvider getProvider() {
    return myProvider;
  }

  public static List<File> getReadOnlyFiles(final Collection<? extends File> affectedFiles) {
    List<File> result = new ArrayList<>();
    for (File file : affectedFiles) {
      if (!file.canWrite()) {
        result.add(file);
      }
    }
    return result;
  }

  public void convertModule(File moduleFile) throws CannotConvertException {
    final ModuleSettings settings = myContext.getModuleSettings(moduleFile);
    myModuleFileConverter.preProcess(settings);
    myModuleFileConverter.process(settings);
    myModuleFileConverter.postProcess(settings);
  }
}
