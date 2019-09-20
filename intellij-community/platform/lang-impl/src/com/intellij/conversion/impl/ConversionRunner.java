// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion.impl;

import com.intellij.conversion.*;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.util.containers.ContainerUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private final List<Path> myModulesFilesToProcess = new ArrayList<>();
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

    myProcessWorkspaceFile = myWorkspaceConverter != null && Files.exists(myContext.getWorkspaceFile())
                             && myWorkspaceConverter.isConversionNeeded(myContext.getWorkspaceSettings());

    myModulesFilesToProcess.clear();
    if (myModuleFileConverter != null) {
      for (Path moduleFile : myContext.getModulePaths()) {
        if (Files.exists(moduleFile) && myModuleFileConverter.isConversionNeeded(myContext.getModuleSettings(moduleFile))) {
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

  public boolean isModuleConversionNeeded(Path moduleFile) throws CannotConvertException {
    return myModuleFileConverter != null && myModuleFileConverter.isConversionNeeded(myContext.getModuleSettings(moduleFile.toFile()));
  }

  public Collection<Path> getCreatedFiles() {
    return ContainerUtil.map(myConverter.getCreatedFiles(), file -> file.toPath());
  }

  public Set<Path> getAffectedFiles() {
    Set<Path> affectedFiles = new HashSet<>();
    if (myProcessProjectFile) {
      affectedFiles.add(myContext.getProjectFile().toPath());
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
        affectedFiles.add(settings.getPath());
      }
    }

    for (File file : myConverter.getAdditionalAffectedFiles()) {
      affectedFiles.add(file.toPath());
    }
    return affectedFiles;
  }

  public void preProcess() throws CannotConvertException {
    if (myProcessProjectFile) {
      myProjectFileConverter.preProcess(myContext.getProjectSettings());
    }

    if (myProcessWorkspaceFile) {
      myWorkspaceConverter.preProcess(myContext.getWorkspaceSettings());
    }

    for (Path moduleFile : myModulesFilesToProcess) {
      myModuleFileConverter.preProcess(myContext.getModuleSettings(moduleFile.toFile()));
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

    for (Path moduleFile : myModulesFilesToProcess) {
      myModuleFileConverter.process(myContext.getModuleSettings(moduleFile.toFile()));
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

    for (Path moduleFile : myModulesFilesToProcess) {
      myModuleFileConverter.postProcess(myContext.getModuleSettings(moduleFile.toFile()));
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

  public static List<Path> getReadOnlyFiles(final Collection<? extends Path> affectedFiles) {
    List<Path> result = new ArrayList<>();
    for (Path file : affectedFiles) {
      if (!Files.isWritable(file)) {
        result.add(file);
      }
    }
    return result;
  }

  public void convertModule(Path moduleFile) throws CannotConvertException {
    final ModuleSettings settings = myContext.getModuleSettings(moduleFile.toFile());
    myModuleFileConverter.preProcess(settings);
    myModuleFileConverter.process(settings);
    myModuleFileConverter.postProcess(settings);
  }
}
