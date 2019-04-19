// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.RunManagerSettings;
import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.execution.impl.RunManagerImplKt.PROJECT_RUN_MANAGER_COMPONENT_NAME;

/**
 * @author nik
 */
public class RunManagerSettingsImpl implements RunManagerSettings {
  @NonNls public static final String RUN_MANAGER_COMPONENT_NAME = "RunManager";
  @NonNls public static final String CONFIGURATION_ELEMENT = "configuration";
  private SettingsXmlFile myWorkspaceFile;
  private SettingsXmlFile myProjectFile;
  private final List<SettingsXmlFile> mySharedConfigurationFiles;

  public RunManagerSettingsImpl(@NotNull File workspaceFile, @Nullable File projectFile, @Nullable File[] sharedConfigurationFiles,
                                ConversionContextImpl context) throws CannotConvertException {
    if (workspaceFile.exists()) {
      myWorkspaceFile = context.getOrCreateFile(workspaceFile);
    }

    if (projectFile != null && projectFile.exists()) {
      myProjectFile = context.getOrCreateFile(projectFile);
    }

    mySharedConfigurationFiles = new ArrayList<>();
    if (sharedConfigurationFiles != null) {
      for (File file : sharedConfigurationFiles) {
        mySharedConfigurationFiles.add(context.getOrCreateFile(file));
      }
    }
  }

  @Override
  @NotNull
  public Collection<? extends Element> getRunConfigurations() {
    final List<Element> result = new ArrayList<>();
    if (myWorkspaceFile != null) {
      result.addAll(JDOMUtil.getChildren(myWorkspaceFile.findComponent(RUN_MANAGER_COMPONENT_NAME), CONFIGURATION_ELEMENT));
    }

    if (myProjectFile != null) {
      result.addAll(JDOMUtil.getChildren(myProjectFile.findComponent(PROJECT_RUN_MANAGER_COMPONENT_NAME), CONFIGURATION_ELEMENT));
    }

    for (SettingsXmlFile file : mySharedConfigurationFiles) {
      result.addAll(JDOMUtil.getChildren(file.getRootElement(), CONFIGURATION_ELEMENT));
    }

    return result;
  }

  public Collection<File> getAffectedFiles() {
    final List<File> files = new ArrayList<>();
    if (myWorkspaceFile != null) {
      files.add(myWorkspaceFile.getFile());
    }
    if (myProjectFile != null) {
      files.add(myProjectFile.getFile());
    }
    for (SettingsXmlFile file : mySharedConfigurationFiles) {
      files.add(file.getFile());
    }
    return files;
  }

}
