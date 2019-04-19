// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.project.ExternalModuleBuildClasspathPojo;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectBuildClasspathPojo;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.PackageDirectoryCache;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Vladislav.Soroka
 */
public class GradleBuildClasspathManager {
  @NotNull
  private final Project myProject;

  @NotNull
  private volatile List<VirtualFile> allFilesCache;

  @NotNull
  private final AtomicReference<Map<String/*module path*/, List<VirtualFile> /*module build classpath*/>> myClasspathMap
    = new AtomicReference<>(new HashMap<>());

  private final Map<String, PackageDirectoryCache> myClassFinderCache = ConcurrentFactoryMap
    .createMap(path -> PackageDirectoryCache.createCache(getModuleClasspathEntries(path)));

  public GradleBuildClasspathManager(@NotNull Project project) {
    myProject = project;
    allFilesCache = ContainerUtil.newArrayList();
  }

  @NotNull
  public static GradleBuildClasspathManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleBuildClasspathManager.class);
  }

  public void reload() {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
    assert manager != null;
    AbstractExternalSystemLocalSettings<?> localSettings = manager.getLocalSettingsProvider().fun(myProject);

    Map<String/*module path*/, List<VirtualFile> /*module build classpath*/> map = ContainerUtil.newHashMap();

    final JarFileSystem jarFileSystem = JarFileSystem.getInstance();
    for (final ExternalProjectBuildClasspathPojo projectBuildClasspathPojo : localSettings.getProjectBuildClasspath().values()) {
      final List<VirtualFile> projectBuildClasspath = ContainerUtil.newArrayList();
      for (String path : projectBuildClasspathPojo.getProjectBuildClasspath()) {
        final VirtualFile virtualFile = ExternalSystemUtil.findLocalFileByPath(path);
        ContainerUtil.addIfNotNull(projectBuildClasspath,
                                   virtualFile == null || virtualFile.isDirectory()
                                   ? virtualFile
                                   : jarFileSystem.getJarRootForLocalFile(virtualFile));
      }

      for (final ExternalModuleBuildClasspathPojo moduleBuildClasspathPojo : projectBuildClasspathPojo.getModulesBuildClasspath().values()) {
        final List<VirtualFile> moduleBuildClasspath = ContainerUtil.newArrayList(projectBuildClasspath);
            for (String path : moduleBuildClasspathPojo.getEntries()) {
              final VirtualFile virtualFile = ExternalSystemUtil.findLocalFileByPath(path);
              ContainerUtil.addIfNotNull(moduleBuildClasspath,
                                         virtualFile == null || virtualFile.isDirectory()
                                         ? virtualFile
                                         : jarFileSystem.getJarRootForLocalFile(virtualFile));
            }

        map.put(moduleBuildClasspathPojo.getPath(), moduleBuildClasspath);
      }
    }

    myClasspathMap.set(map);

    Set<VirtualFile> set = new LinkedHashSet<>();
    for (List<VirtualFile> virtualFiles : myClasspathMap.get().values()) {
      set.addAll(virtualFiles);
    }
    allFilesCache = ContainerUtil.newArrayList(set);
    myClassFinderCache.clear();
  }

  public Map<String, PackageDirectoryCache> getClassFinderCache() {
    return myClassFinderCache;
  }

  @NotNull
  public List<VirtualFile> getAllClasspathEntries() {
    checkRootsValidity(allFilesCache);
    return allFilesCache;
  }

  @NotNull
  public List<VirtualFile> getModuleClasspathEntries(@NotNull String externalModulePath) {
    checkRootsValidity(myClasspathMap.get().get(externalModulePath));
    List<VirtualFile> virtualFiles = myClasspathMap.get().get(externalModulePath);
    return virtualFiles == null ? Collections.emptyList() : virtualFiles;
  }

  private void checkRootsValidity(@Nullable List<VirtualFile> virtualFiles) {
    if (virtualFiles == null) return;

    if (!virtualFiles.isEmpty()) {
      for (VirtualFile file : virtualFiles) {
        if (!file.isValid()) {
          reload();
          break;
        }
      }
    }
  }
}
