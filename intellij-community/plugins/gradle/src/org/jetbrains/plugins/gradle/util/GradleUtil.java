// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileTypeDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileFilters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.BooleanFunction;
import com.intellij.util.containers.Stack;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.gradle.GradleScript;
import org.gradle.util.GUtil;
import org.gradle.wrapper.WrapperConfiguration;
import org.gradle.wrapper.WrapperExecutor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import static com.intellij.openapi.util.text.StringUtil.*;
import static org.jetbrains.plugins.gradle.util.GradleConstants.EXTENSION;
import static org.jetbrains.plugins.gradle.util.GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION;

/**
 * Holds miscellaneous utility methods.
 *
 * @author Denis Zhdanov
 */
public class GradleUtil {
  private static final String LAST_USED_GRADLE_HOME_KEY = "last.used.gradle.home";

  private GradleUtil() { }

  /**
   * Allows to retrieve file chooser descriptor that filters gradle scripts.
   * <p/>
   * <b>Note:</b> we want to fall back to the standard {@link FileTypeDescriptor} when dedicated gradle file type
   * is introduced (it's processed as groovy file at the moment). We use open project descriptor here in order to show
   * custom gradle icon at the file chooser ({@link icons.GradleIcons#Gradle}, is used at the file chooser dialog via
   * the dedicated gradle project open processor).
   */
  @NotNull
  public static FileChooserDescriptor getGradleProjectFileChooserDescriptor() {
    return new FileChooserDescriptor(true, false, false, false, false, false)
      .withFileFilter(file -> SystemInfo.isFileSystemCaseSensitive
                              ? endsWith(file.getName(), "." + EXTENSION) || endsWith(file.getName(), "." + KOTLIN_DSL_SCRIPT_EXTENSION)
                              : endsWithIgnoreCase(file.getName(), "." + EXTENSION) || endsWithIgnoreCase(file.getName(), "." + KOTLIN_DSL_SCRIPT_EXTENSION));
  }

  @NotNull
  public static FileChooserDescriptor getGradleHomeFileChooserDescriptor() {
    // allow selecting files to avoid confusion:
    // on macOS a user can select any file but after clicking OK, dialog is closed, but IDEA doesnt' receive the file and doesn't react
    return FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor();
  }

  public static boolean isGradleDefaultWrapperFilesExist(@Nullable String gradleProjectPath) {
    return getWrapperConfiguration(gradleProjectPath) != null;
  }

  /**
   * Tries to retrieve what settings should be used with gradle wrapper for the gradle project located at the given path.
   *
   * @param gradleProjectPath  target gradle project config (*.gradle) path or config file's directory path.
   * @return                   gradle wrapper settings should be used with gradle wrapper for the gradle project located at the given path
   *                           if any; {@code null} otherwise
   */
  @Nullable
  public static WrapperConfiguration getWrapperConfiguration(@Nullable String gradleProjectPath) {
    final File wrapperPropertiesFile = findDefaultWrapperPropertiesFile(gradleProjectPath);
    if (wrapperPropertiesFile == null) return null;

    final WrapperConfiguration wrapperConfiguration = new WrapperConfiguration();
    try {
      final Properties props = GUtil.loadProperties(wrapperPropertiesFile);
      String distributionUrl = props.getProperty(WrapperExecutor.DISTRIBUTION_URL_PROPERTY);
      if(isEmpty(distributionUrl)) {
        throw new ExternalSystemException("Wrapper 'distributionUrl' property does not exist!");
      } else {
        wrapperConfiguration.setDistribution(prepareDistributionUri(distributionUrl, wrapperPropertiesFile));
      }
      String distributionPath = props.getProperty(WrapperExecutor.DISTRIBUTION_PATH_PROPERTY);
      if(!isEmpty(distributionPath)) {
        wrapperConfiguration.setDistributionPath(distributionPath);
      }
      String distPathBase = props.getProperty(WrapperExecutor.DISTRIBUTION_BASE_PROPERTY);
      if(!isEmpty(distPathBase)) {
        wrapperConfiguration.setDistributionBase(distPathBase);
      }
      String zipStorePath = props.getProperty(WrapperExecutor.ZIP_STORE_PATH_PROPERTY);
      if(!isEmpty(zipStorePath)) {
        wrapperConfiguration.setZipPath(zipStorePath);
      }
      String zipStoreBase = props.getProperty(WrapperExecutor.ZIP_STORE_BASE_PROPERTY);
      if(!isEmpty(zipStoreBase)) {
        wrapperConfiguration.setZipBase(zipStoreBase);
      }
      return wrapperConfiguration;
    }
    catch (Exception e) {
      GradleLog.LOG.warn(
        String.format("I/O exception on reading gradle wrapper properties file at '%s'", wrapperPropertiesFile.getAbsolutePath()), e);
    }
    return null;
  }

  private static URI prepareDistributionUri(String distributionUrl, File propertiesFile) throws URISyntaxException {
    URI source = new URI(distributionUrl);
    return source.getScheme() != null ? source : new File(propertiesFile.getParentFile(), source.getSchemeSpecificPart()).toURI();
  }

  /**
   * Allows to build file system path to the target gradle sub-project given the root project path.
   *
   * @param subProject       target sub-project which config path we're interested in
   * @param rootProjectPath  path to root project's directory which contains 'build.gradle'
   * @return                 path to the given sub-project's directory which contains 'build.gradle'
   */
  @NotNull
  public static String getConfigPath(@NotNull GradleProject subProject, @NotNull String rootProjectPath) {
    try {
      GradleScript script = subProject.getBuildScript();
      if (script != null) {
        File file = script.getSourceFile();
        if (file != null) {
          if (!file.isDirectory()) {
            // The file points to 'build.gradle' at the moment but we keep it's parent dir path instead.
            file = file.getParentFile();
          }
          return ExternalSystemApiUtil.toCanonicalPath(file.getCanonicalPath());
        }
      }
    }
    catch (Exception e) {
      // As said by gradle team: 'One thing I'm interested in is whether you have any thoughts about how the tooling API should
      // deal with missing details from the model - for example, when asking for details about the build scripts when using
      // a version of Gradle that does not supply that information. Currently, you'll get a `UnsupportedOperationException`
      // when you call the `getBuildScript()` method'.
      //
      // So, just ignore it and assume that the user didn't define any custom build file name.
    }
    File rootProjectParent = new File(rootProjectPath);
    StringBuilder buffer = new StringBuilder(FileUtil.toCanonicalPath(rootProjectParent.getAbsolutePath()));
    Stack<String> stack = new Stack<>();
    for (GradleProject p = subProject; p != null; p = p.getParent()) {
      stack.push(p.getName());
    }

    // pop root project
    stack.pop();
    while (!stack.isEmpty()) {
      buffer.append(ExternalSystemConstants.PATH_SEPARATOR).append(stack.pop());
    }
    return buffer.toString();
  }

  @NotNull
  public static String getLastUsedGradleHome() {
    return PropertiesComponent.getInstance().getValue(LAST_USED_GRADLE_HOME_KEY, "");
  }

  public static void storeLastUsedGradleHome(@Nullable String gradleHomePath) {
    PropertiesComponent.getInstance().setValue(LAST_USED_GRADLE_HOME_KEY, gradleHomePath, null);
  }

  @Nullable
  public static File findDefaultWrapperPropertiesFile(@Nullable String gradleProjectPath) {
    if (gradleProjectPath == null) {
      return null;
    }
    File file = new File(gradleProjectPath);

    // There is a possible case that given path points to a gradle script (*.gradle) but it's also possible that
    // it references script's directory. We want to provide flexibility here.
    File gradleDir;
    if (file.isFile()) {
      gradleDir = new File(file.getParentFile(), "gradle");
    }
    else {
      gradleDir = new File(file, "gradle");
    }
    if (!gradleDir.isDirectory()) {
      return null;
    }

    File wrapperDir = new File(gradleDir, "wrapper");
    if (!wrapperDir.isDirectory()) {
      return null;
    }

    File[] candidates = wrapperDir.listFiles(FileFilters.filesWithExtension("properties"));
    if (candidates == null) {
      GradleLog.LOG.warn("No *.properties file is found at the gradle wrapper directory " + wrapperDir.getAbsolutePath());
      return null;
    }
    else if (candidates.length != 1) {
      GradleLog.LOG.warn(String.format(
        "%d *.properties files instead of one have been found at the wrapper directory (%s): %s",
        candidates.length, wrapperDir.getAbsolutePath(), Arrays.toString(candidates)
      ));
      return null;
    }

    return candidates[0];
  }

  @NotNull
  public static String determineRootProject(@NotNull String subProjectPath) {
    final Path subProject = Paths.get(subProjectPath);
    Path candidate = subProject;
    try {
      while (candidate != null && candidate != candidate.getParent()) {
        if (containsGradleSettingsFile(candidate)) {
          return candidate.toString();
        }
        candidate = candidate.getParent();
      }
    } catch (IOException e) {
      GradleLog.LOG.warn("Failed to determine root Gradle project directory for [" + subProjectPath + "]", e);
    }
    return Files.isDirectory(subProject) ? subProjectPath : subProject.getParent().toString();
  }

  private static boolean containsGradleSettingsFile(Path directory) throws IOException {
    return Files.isDirectory(directory) && Files.walk(directory, 1)
      .map(Path::getFileName)
      .filter(Objects::nonNull)
      .map(Path::toString)
      .anyMatch(name -> name.startsWith("settings.gradle"));
  }

  /**
   * Finds real external module data by ide module
   *
   * Module 'module' -> ModuleData 'module'
   * Module 'module.main' -> ModuleData 'module' instead of GradleSourceSetData 'module.main'
   * Module 'module.test' -> ModuleData 'module' instead of GradleSourceSetData 'module.test'
   */
  @ApiStatus.Experimental
  @Nullable
  public static DataNode<ModuleData> findGradleModuleData(@NotNull Module module) {
    String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    if (projectPath == null) return null;
    Project project = module.getProject();
    return findGradleModuleData(project, projectPath);
  }

  @ApiStatus.Experimental
  @Nullable
  public static DataNode<ModuleData> findGradleModuleData(@NotNull Project project, @NotNull String projectPath) {
    DataNode<ProjectData> projectNode = ExternalSystemApiUtil.findProjectData(project, GradleConstants.SYSTEM_ID, projectPath);
    if (projectNode == null) return null;
    BooleanFunction<DataNode<ModuleData>> predicate = node -> projectPath.equals(node.getData().getLinkedExternalProjectPath());
    return ExternalSystemApiUtil.find(projectNode, ProjectKeys.MODULE, predicate);
  }
}
