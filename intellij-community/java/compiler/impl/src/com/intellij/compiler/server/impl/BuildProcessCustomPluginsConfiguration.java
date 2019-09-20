// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server.impl;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores list of project-specific plugins which should be added to the build process' classpath. This can be used to provide custom build
 * steps for a project.
 * <p/>
 * <strong>This is an experimental option and it isn't available via UI at the moment. It may be changed or removed at any time.</strong>
 */
@State(name = "BuildProcessPlugins", storages = @Storage("compiler.xml"))
public class BuildProcessCustomPluginsConfiguration implements PersistentStateComponent<BuildProcessCustomPluginsConfiguration.BuildProcessPluginsState> {
  private static final Logger LOG = Logger.getInstance(BuildProcessCustomPluginsConfiguration.class);
  private final BuildProcessPluginsState myState = new BuildProcessPluginsState();
  private final Project myProject;

  public BuildProcessCustomPluginsConfiguration(Project project) {
    myProject = project;
  }

  public static BuildProcessCustomPluginsConfiguration getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, BuildProcessCustomPluginsConfiguration.class);
  }

  public List<String> getProjectLibraries() {
    return myState.myProjectLibraries;
  }

  public List<String> getCustomPluginsClasspath() {
    return ReadAction.compute(() -> {
      List<String> result = new ArrayList<>();
      LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);
      for (String libraryName : myState.myProjectLibraries) {
        Library library = libraryTable.getLibraryByName(libraryName);
        if (library == null) {
          LOG.warn("Unknown project library in BuildProcessCustomPluginsConfiguration: " + libraryName);
          continue;
        }
        for (VirtualFile file : library.getFiles(OrderRootType.CLASSES)) {
          result.add(VfsUtilCore.virtualToIoFile(file).getAbsolutePath());
        }
      }
      return result;
    });
  }

  @Nullable
  @Override
  public BuildProcessPluginsState getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull BuildProcessPluginsState state) {
    XmlSerializerUtil.copyBean(state, myState);
  }

  public static class BuildProcessPluginsState {
    @Property(surroundWithTag = false)
    @XCollection(elementName = "project-library", valueAttributeName = "name", style = XCollection.Style.v2)
    public List<String> myProjectLibraries = new ArrayList<>();
  }
}
