// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.initialization.BuildLayoutParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Denis Zhdanov
 */
public class GradleAutoImportAware implements ExternalSystemAutoImportAware {
  private static final Logger LOG = Logger.getInstance(GradleAutoImportAware.class);

  @Nullable
  @Override
  public String getAffectedExternalProjectPath(@NotNull String changedFileOrDirPath, @NotNull Project project) {
    if (!changedFileOrDirPath.endsWith("." + GradleConstants.EXTENSION) &&
        !changedFileOrDirPath.endsWith("." + GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION)) {
      return null;
    }

    if (isInsideCompileOutput(changedFileOrDirPath, project)) {
      return null;
    }

    File file = new File(changedFileOrDirPath);
    if (file.isDirectory()) {
      return null;
    }

    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
    assert manager != null;
    AbstractExternalSystemSettings<?, ?,?> systemSettings = manager.getSettingsProvider().fun(project);
    Collection<? extends ExternalProjectSettings> projectsSettings = systemSettings.getLinkedProjectsSettings();
    if (projectsSettings.isEmpty()) {
      return null;
    }
    Map<String, String> rootPaths = new HashMap<>();
    for (ExternalProjectSettings setting : projectsSettings) {
      if(setting != null) {
        for (String path : setting.getModules()) {
          rootPaths.put(new File(path).getAbsolutePath(), setting.getExternalProjectPath());
        }
      }
    }

    for (File f = file.getParentFile(); f != null; f = f.getParentFile()) {
      String dirPath = f.getAbsolutePath();
      if (rootPaths.containsKey(dirPath)) {
        return rootPaths.get(dirPath);
      }
    }
    return null;
  }

  private static boolean isInsideCompileOutput(@NotNull String path, @NotNull Project project) {
    final String url = VfsUtilCore.pathToUrl(path);

    boolean isInsideProjectCompile =
      Optional.ofNullable(CompilerProjectExtension.getInstance(project))
              .map(CompilerProjectExtension::getCompilerOutputUrl)
              .filter(outputUrl -> VfsUtilCore.isEqualOrAncestor(outputUrl, url))
              .isPresent();

    if (isInsideProjectCompile) {
      return true;
    }

    return
      Arrays.stream(ModuleManager.getInstance(project).getModules())
                                                  .map(CompilerModuleExtension::getInstance)
                                                  .filter(Objects::nonNull)
                                                  .flatMap(ex -> Stream.of(ex.getCompilerOutputUrl(), ex.getCompilerOutputUrlForTests()))
                                                  .filter(Objects::nonNull)
                                                  .anyMatch(outputUrl -> VfsUtilCore.isEqualOrAncestor(outputUrl, url));
  }

  @Override
  public List<File> getAffectedExternalProjectFiles(String projectPath, @NotNull Project project) {
    final List<File> files = new SmartList<>();

    // add global gradle.properties
    String serviceDirectoryPath = GradleSettings.getInstance(project).getServiceDirectoryPath();
    File gradleUserHomeDir = new BuildLayoutParameters().getGradleUserHomeDir();
    files.add(new File(serviceDirectoryPath != null ? serviceDirectoryPath : gradleUserHomeDir.getPath(), "gradle.properties"));
    // add init script
    files.add(new File(serviceDirectoryPath != null ? serviceDirectoryPath : gradleUserHomeDir.getPath(), "init.gradle"));
    // TODO add init scripts from USER_HOME/.gradle/init.d/ directory

    // add project-specific gradle.properties
    GradleProjectSettings projectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(projectPath);
    files.add(new File(projectSettings == null ? projectPath : projectSettings.getExternalProjectPath(), "gradle.properties"));

    // add wrapper config file
    if (projectSettings != null && projectSettings.getDistributionType() == DistributionType.DEFAULT_WRAPPED) {
      files.add(new File(projectSettings.getExternalProjectPath(), "gradle/wrapper/gradle-wrapper.properties"));
    }

    // add gradle scripts
    Set<String> subProjectPaths = projectSettings != null && /*!projectSettings.getModules().isEmpty() &&*/
                                  FileUtil.pathsEqual(projectSettings.getExternalProjectPath(), projectPath)
                                  ? projectSettings.getModules() : ContainerUtil.set(projectPath);
    for (String path : subProjectPaths) {
      ProgressManager.checkCanceled();

      try {
        Files.walkFileTree(Paths.get(path), EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            String fileName = path.getFileName().toString();
            if (fileName.endsWith('.' + GradleConstants.EXTENSION) ||
                fileName.endsWith('.' + GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION)) {
              File file = path.toFile();
              if (file.isFile()) files.add(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });
      }
      catch (IOException | InvalidPathException e) {
        LOG.debug(e);
      }
    }

    return files;
  }

  @Override
  public boolean isApplicable(@Nullable ProjectResolverPolicy resolverPolicy) {
    return resolverPolicy == null || !resolverPolicy.isPartialDataResolveAllowed();
  }
}
