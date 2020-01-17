// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalSourceDirectorySet;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;

import java.io.File;
import java.util.*;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.find;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.attachGradleSdkSources;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.attachSourcesAndJavadocFromGradleCacheIfNeeded;

/**
 * {@link LibraryDataNodeSubstitutor} provides the facility to replace library dependencies with the related module dependencies
 * based on artifacts and source compilation output mapping
 */
@ApiStatus.Internal
public class LibraryDataNodeSubstitutor {
  private @NotNull final ProjectResolverContext resolverContext;
  private @Nullable final File gradleUserHomeDir;
  private @Nullable final File gradleHomeDir;
  private @Nullable final GradleVersion gradleVersion;
  private @NotNull final Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap;
  private @NotNull final Map<String, Pair<String, ExternalSystemSourceType>> moduleOutputsMap;
  private @NotNull final Map<String, String> artifactsMap;

  public LibraryDataNodeSubstitutor(@NotNull ProjectResolverContext context,
                                    @Nullable File gradleUserHomeDir,
                                    @Nullable File gradleHomeDir,
                                    @Nullable GradleVersion gradleVersion,
                                    @NotNull Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap,
                                    @NotNull Map<String, Pair<String, ExternalSystemSourceType>> moduleOutputsMap,
                                    @NotNull Map<String, String> artifactsMap) {
    resolverContext = context;
    this.gradleUserHomeDir = gradleUserHomeDir;
    this.gradleHomeDir = gradleHomeDir;
    this.gradleVersion = gradleVersion;
    this.sourceSetMap = sourceSetMap;
    this.moduleOutputsMap = moduleOutputsMap;
    this.artifactsMap = artifactsMap;
  }

  public void run(@NotNull DataNode<LibraryDependencyData> libraryDependencyDataNode) {
    final DataNode<?> libraryNodeParent = libraryDependencyDataNode.getParent();
    if (libraryNodeParent == null) return;

    final LibraryDependencyData libraryDependencyData = libraryDependencyDataNode.getData();
    final LibraryData libraryData = libraryDependencyData.getTarget();
    final Set<String> libraryPaths = libraryData.getPaths(LibraryPathType.BINARY);
    if (libraryPaths.isEmpty()) return;
    if (StringUtil.isNotEmpty(libraryData.getExternalName())) {
      if (gradleUserHomeDir != null) {
        attachSourcesAndJavadocFromGradleCacheIfNeeded(resolverContext, gradleUserHomeDir, libraryData);
      }
      return;
    }

    boolean shouldKeepTransitiveDependencies = libraryPaths.size() > 0 && !libraryDependencyDataNode.getChildren().isEmpty();

    final LinkedList<String> unprocessedPaths = new LinkedList<>(libraryPaths);
    while (!unprocessedPaths.isEmpty()) {
      final String path = unprocessedPaths.remove();

      Set<String> targetModuleOutputPaths = null;

      final String moduleId;
      final Pair<String, ExternalSystemSourceType> sourceTypePair = moduleOutputsMap.get(path);
      if (sourceTypePair == null) {
        moduleId = artifactsMap.get(path);
        if (moduleId != null) {
          targetModuleOutputPaths = ContainerUtil.set(path);
        }
      }
      else {
        moduleId = sourceTypePair.first;
      }
      if (moduleId == null) continue;

      final Pair<DataNode<GradleSourceSetData>, ExternalSourceSet> pair = sourceSetMap.get(moduleId);
      if (pair == null) {
        continue;
      }

      final ModuleData moduleData = pair.first.getData();
      if (targetModuleOutputPaths == null) {
        final Set<String> compileSet = new HashSet<>();
        MultiMap<ExternalSystemSourceType, String> gradleOutputs = pair.first.getUserData(GradleProjectResolver.GRADLE_OUTPUTS);
        if (gradleOutputs != null) {
          ContainerUtil.addAllNotNull(compileSet,
                                      gradleOutputs.get(ExternalSystemSourceType.SOURCE));
          ContainerUtil.addAllNotNull(compileSet,
                                      gradleOutputs.get(ExternalSystemSourceType.RESOURCE));
        }
        if (!compileSet.isEmpty() && ContainerUtil.intersects(libraryPaths, compileSet)) {
          targetModuleOutputPaths = compileSet;
        }
        else {
          final Set<String> testSet = new HashSet<>();
          if (gradleOutputs != null) {
            ContainerUtil.addAllNotNull(testSet,
                                        gradleOutputs.get(ExternalSystemSourceType.TEST));
            ContainerUtil.addAllNotNull(testSet,
                                        gradleOutputs.get(ExternalSystemSourceType.TEST_RESOURCE));
          }
          if (!testSet.isEmpty() && ContainerUtil.intersects(libraryPaths, testSet)) {
            targetModuleOutputPaths = testSet;
          }
        }
      }

      final ModuleData ownerModule = libraryDependencyData.getOwnerModule();
      final ModuleDependencyData moduleDependencyData = new ModuleDependencyData(ownerModule, moduleData);
      moduleDependencyData.setScope(libraryDependencyData.getScope());
      if ("test".equals(pair.second.getName())) {
        moduleDependencyData.setProductionOnTestDependency(true);
      }
      final DataNode<ModuleDependencyData> found = find(
        libraryNodeParent, ProjectKeys.MODULE_DEPENDENCY, node -> {
          if (moduleDependencyData.getInternalName().equals(node.getData().getInternalName())) {
            moduleDependencyData.setModuleDependencyArtifacts(node.getData().getModuleDependencyArtifacts());
          }

          final boolean result;
          // ignore provided scope during the search since it can be resolved incorrectly for file dependencies on a source set outputs
          if (moduleDependencyData.getScope() == DependencyScope.PROVIDED) {
            moduleDependencyData.setScope(node.getData().getScope());
            result = moduleDependencyData.equals(node.getData());
            moduleDependencyData.setScope(DependencyScope.PROVIDED);
          }
          else {
            result = moduleDependencyData.equals(node.getData());
          }
          return result;
        });

      if (targetModuleOutputPaths != null) {
        if (found == null) {
          DataNode<ModuleDependencyData> moduleDependencyNode =
            libraryNodeParent.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
          if (shouldKeepTransitiveDependencies) {
            for (DataNode<?> node : libraryDependencyDataNode.getChildren()) {
              moduleDependencyNode.addChild(node);
            }
          }
        }
        libraryPaths.removeAll(targetModuleOutputPaths);
        unprocessedPaths.removeAll(targetModuleOutputPaths);
        if (libraryPaths.isEmpty()) {
          libraryDependencyDataNode.clear(true);
          break;
        }
        continue;
      }
      else {
        // do not add the path as library dependency if another module dependency is already contain the path as one of its output paths
        if (found != null) {
          libraryPaths.remove(path);
          if (libraryPaths.isEmpty()) {
            libraryDependencyDataNode.clear(true);
            break;
          }
          continue;
        }
      }

      final ExternalSourceDirectorySet directorySet = pair.second.getSources().get(sourceTypePair.second);
      if (directorySet != null) {
        for (File file : directorySet.getSrcDirs()) {
          libraryData.addPath(LibraryPathType.SOURCE, file.getAbsolutePath());
        }
      }
    }

    if (libraryDependencyDataNode.getParent() != null) {
      if (libraryPaths.size() > 1) {
        List<String> toRemove = new SmartList<>();
        for (String path : libraryPaths) {
          final File binaryPath = new File(path);
          if (binaryPath.isFile()) {
            final LibraryData extractedLibrary = new LibraryData(libraryDependencyData.getOwner(), "");
            extractedLibrary.addPath(LibraryPathType.BINARY, path);
            if (gradleHomeDir != null && gradleVersion != null) {
              attachGradleSdkSources(binaryPath, extractedLibrary, gradleHomeDir, gradleVersion);
            }
            LibraryDependencyData extractedDependencyData = new LibraryDependencyData(
              libraryDependencyData.getOwnerModule(), extractedLibrary, LibraryLevel.MODULE);
            libraryDependencyDataNode.getParent().createChild(ProjectKeys.LIBRARY_DEPENDENCY, extractedDependencyData);

            toRemove.add(path);
          }
        }
        libraryPaths.removeAll(toRemove);
        if (libraryPaths.isEmpty()) {
          libraryDependencyDataNode.clear(true);
        }
      }
    }
  }
}
