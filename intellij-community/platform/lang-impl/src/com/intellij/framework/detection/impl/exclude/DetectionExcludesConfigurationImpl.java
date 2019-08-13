// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.framework.detection.impl.exclude;

import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.framework.detection.impl.exclude.old.OldFacetDetectionExcludesConfiguration;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerContainer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author nik
 */
@State(name = "FrameworkDetectionExcludesConfiguration")
public class DetectionExcludesConfigurationImpl extends DetectionExcludesConfiguration
         implements PersistentStateComponent<ExcludesConfigurationState>, Disposable {
  private final Map<String, VirtualFilePointerContainer> myExcludedFiles;
  private final Set<String> myExcludedFrameworks;
  private final Project myProject;
  private final VirtualFilePointerManager myPointerManager;
  private boolean myDetectionEnabled = true;
  private boolean myConverted;

  public DetectionExcludesConfigurationImpl(Project project, VirtualFilePointerManager pointerManager) {
    myProject = project;
    myPointerManager = pointerManager;
    myExcludedFrameworks = new HashSet<>();
    myExcludedFiles = FactoryMap.create(key -> myPointerManager.createContainer(this));
  }

  @Override
  public void addExcludedFramework(@NotNull FrameworkType type) {
    convert();
    myExcludedFrameworks.add(type.getId());
    final VirtualFilePointerContainer container = myExcludedFiles.remove(type.getId());
    if (container != null) {
      container.clear();
    }
  }

  @Override
  public void addExcludedFile(@NotNull VirtualFile file, @Nullable FrameworkType type) {
    convert();
    final String typeId = type != null ? type.getId() : null;
    if (typeId != null && myExcludedFrameworks.contains(typeId) || isFileExcluded(file, typeId)) {
      return;
    }

    final VirtualFilePointerContainer container = myExcludedFiles.get(typeId);
    if (typeId == null) {
      for (VirtualFilePointerContainer pointerContainer : myExcludedFiles.values()) {
        removeDescendants(file, pointerContainer);
      }
    }
    else {
      removeDescendants(file, container);
    }
    container.add(file);
  }

  @Override
  public void addExcludedUrl(@NotNull String url, @Nullable FrameworkType type) {
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
    if (file != null) {
      addExcludedFile(file, type);
      return;
    }

    convert();
    final String typeId = type != null ? type.getId() : null;
    if (typeId != null && myExcludedFrameworks.contains(typeId)) {
      return;
    }
    myExcludedFiles.get(typeId).add(url);
  }

  private void convert() {
    ensureOldSettingsLoaded();
    markAsConverted();
  }

  private void markAsConverted() {
    myConverted = true;
    OldFacetDetectionExcludesConfiguration.getInstance(myProject).unsetState();
  }

  private void ensureOldSettingsLoaded() {
    if (!myConverted) {
      final OldFacetDetectionExcludesConfiguration oldConfiguration = OldFacetDetectionExcludesConfiguration.getInstance(myProject);
      final ExcludesConfigurationState oldState = oldConfiguration.convert();
      if (oldState != null) {
        doLoadState(oldState);
      }
    }
  }

  @Override
  public boolean isExcludedFromDetection(@NotNull VirtualFile file, @NotNull FrameworkType frameworkType) {
    ensureOldSettingsLoaded();
    return isExcludedFromDetection(frameworkType) || isFileExcluded(file, frameworkType.getId());
  }

  @Override
  public boolean isExcludedFromDetection(@NotNull FrameworkType frameworkType) {
    ensureOldSettingsLoaded();
    return !myDetectionEnabled || myExcludedFrameworks.contains(frameworkType.getId());
  }

  private boolean isFileExcluded(@NotNull VirtualFile file, @Nullable String typeId) {
    if (myExcludedFiles.containsKey(typeId) && isUnder(file, myExcludedFiles.get(typeId))) return true;
    return typeId != null && myExcludedFiles.containsKey(null) && isUnder(file, myExcludedFiles.get(null));
  }

  private static boolean isUnder(VirtualFile file, final VirtualFilePointerContainer container) {
    for (VirtualFile excludedFile : container.getFiles()) {
      if (VfsUtilCore.isAncestor(excludedFile, file, false)) {
        return true;
      }
    }
    return false;
  }

  private void removeDescendants(VirtualFile file, VirtualFilePointerContainer container) {
    for (VirtualFile virtualFile : container.getFiles()) {
      if (VfsUtilCore.isAncestor(file, virtualFile, false)) {
        container.remove(myPointerManager.create(virtualFile, this, null));
      }
    }
  }

  public void removeExcluded(@NotNull Collection<VirtualFile> files, final FrameworkType frameworkType) {
    ensureOldSettingsLoaded();
    if (!myDetectionEnabled || myExcludedFrameworks.contains(frameworkType.getId())) {
      files.clear();
      return;
    }

    final Iterator<VirtualFile> iterator = files.iterator();
    while (iterator.hasNext()) {
      VirtualFile file = iterator.next();
      if (isFileExcluded(file, frameworkType.getId())) {
        iterator.remove();
      }
    }
  }

  @NotNull
  public ExcludesConfigurationState getActualState() {
    ensureOldSettingsLoaded();

    final ExcludesConfigurationState state = new ExcludesConfigurationState();
    state.setDetectionEnabled(myDetectionEnabled);
    state.getFrameworkTypes().addAll(myExcludedFrameworks);
    Collections.sort(state.getFrameworkTypes(), String.CASE_INSENSITIVE_ORDER);

    for (String typeId : myExcludedFiles.keySet()) {
      final VirtualFilePointerContainer container = myExcludedFiles.get(typeId);
      for (String url : container.getUrls()) {
        state.getFiles().add(new ExcludedFileState(url, typeId));
      }
    }
    Collections.sort(state.getFiles(),
                     (o1, o2) -> StringUtil.comparePairs(o1.getFrameworkType(), o1.getUrl(), o2.getFrameworkType(), o2.getUrl(), true));
    return state;
  }

  @Override @Nullable
  public ExcludesConfigurationState getState() {
    if (!myConverted) return null;
    return getActualState();
  }

  @Override
  public void loadState(@NotNull ExcludesConfigurationState state) {
    doLoadState(state);
    if (!myExcludedFiles.isEmpty() || !myExcludedFrameworks.isEmpty() || !myDetectionEnabled) {
      markAsConverted();
    }
  }

  private void doLoadState(@Nullable ExcludesConfigurationState state) {
    myExcludedFrameworks.clear();
    for (VirtualFilePointerContainer container : myExcludedFiles.values()) {
      container.clear();
    }
    myDetectionEnabled = state == null || state.isDetectionEnabled();
    if (state != null) {
      myExcludedFrameworks.addAll(state.getFrameworkTypes());
      for (ExcludedFileState fileState : state.getFiles()) {
        myExcludedFiles.get(fileState.getFrameworkType()).add(fileState.getUrl());
      }
    }
  }

  @Override
  public void dispose() {
  }
}
