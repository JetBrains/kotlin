// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util;

import com.intellij.codeInsight.AttachSourcesProvider;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.initialization.BuildLayoutParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.intellij.jarFinder.InternetAttachSourceProvider.attachSourceJar;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.attachSourcesAndJavadocFromGradleCacheIfNeeded;

/**
 * @author Vladislav.Soroka
 */
public class GradleAttachSourcesProvider implements AttachSourcesProvider {
  @NotNull
  @Override
  public Collection<AttachSourcesAction> getActions(List<LibraryOrderEntry> orderEntries, PsiFile psiFile) {
    Map<LibraryOrderEntry, Module> gradleModules = getGradleModules(orderEntries);
    if (gradleModules.isEmpty()) return Collections.emptyList();

    return Collections.singleton(new AttachSourcesAction() {
      @Override
      public String getName() {
        return GradleBundle.message("gradle.action.download.sources");
      }

      @Override
      public String getBusyText() {
        return GradleBundle.message("gradle.action.download.sources.busy.text");
      }

      @Override
      public ActionCallback perform(List<LibraryOrderEntry> orderEntries) {
        Map<LibraryOrderEntry, Module> gradleModules = getGradleModules(orderEntries);
        if (gradleModules.isEmpty()) return ActionCallback.REJECTED;
        final ActionCallback resultWrapper = new ActionCallback();
        Project project = psiFile.getProject();

        Map.Entry<LibraryOrderEntry, Module> next = gradleModules.entrySet().iterator().next();
        LibraryOrderEntry libraryOrderEntry = next.getKey();
        Module module = next.getValue();

        String libraryName = libraryOrderEntry.getLibraryName();
        if (libraryName == null) return ActionCallback.REJECTED;

        String artifactCoordinates = StringUtil.trimStart(libraryName, GradleConstants.SYSTEM_ID.getReadableName() + ": ");
        if (StringUtil.equals(libraryName, artifactCoordinates)) return ActionCallback.REJECTED;
        final String gradlePath = GradleProjectResolverUtil.getGradlePath(module);
        if (gradlePath == null) return ActionCallback.REJECTED;

        final String sourcesLocationFilePath;
        final File sourcesLocationFile;
        try {
          sourcesLocationFile = new File(FileUtil.createTempDirectory("sources", "loc"), "path.tmp");
          sourcesLocationFilePath = StringUtil.escapeBackSlashes(sourcesLocationFile.getCanonicalPath());
          Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtil.delete(sourcesLocationFile), "GradleAttachSourcesProvider cleanup"));
        }
        catch (IOException e) {
          GradleLog.LOG.warn(e);
          return ActionCallback.REJECTED;
        }
        final String taskName = "DownloadSources";
        // @formatter:off
        String initScript = "allprojects {\n" +
                            "  afterEvaluate { project ->\n" +
                            "    if(project.path == '" + gradlePath + "') {\n" +
                            "        def overwrite = project.tasks.findByName('" + taskName + "') != null\n" +
                            "        project.tasks.create(name: '" + taskName + "', overwrite: overwrite) {\n" +
                            "        doLast {\n" +
                            "          def configuration = null\n" +
                            "          def repository = project.repositories.toList().find {\n" +
                            "              project.repositories.clear()\n" +
                            "              project.repositories.add(it)\n" +
                            "              configuration = project.configurations.create('downloadSourcesFrom_' + it.name + '_' + UUID.randomUUID())\n" +
                            "              configuration.transitive = false\n" +
                            "              project.dependencies.add(configuration.name, '" + artifactCoordinates + ":sources" + "')\n" +
                            "              configuration.resolvedConfiguration.lenientConfiguration.getFiles().any()\n" +
                            "          }\n" +
                            "          if (!repository) {\n" +
                            "              configuration = project.configurations.create('downloadSources_' + UUID.randomUUID())\n" +
                            "              configuration.transitive = false\n" +
                            "              project.dependencies.add(configuration.name, '" + artifactCoordinates + ":sources" + "')\n" +
                            "              configuration.resolve()\n" +
                            "          }\n" +
                            "          new File('" + sourcesLocationFilePath + "').write configuration?.singleFile?.path\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}\n";
        // @formatter:on
        UserDataHolderBase userData = new UserDataHolderBase();
        userData.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, initScript);

        String gradleVmOptions = GradleSettings.getInstance(project).getGradleVmOptions();
        ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
        settings.setExecutionName("Download sources");
        settings.setExternalProjectPath(ExternalSystemApiUtil.getExternalRootProjectPath(module));
        settings.setTaskNames(Collections.singletonList(taskName));
        settings.setVmOptions(gradleVmOptions);
        settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
        ExternalSystemUtil.runTask(
          settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
          new TaskCallback() {
            @Override
            public void onSuccess() {
              VirtualFile classesFile = libraryOrderEntry.getFiles(OrderRootType.CLASSES)[0];
              File sourceJar = getSourceFile(artifactCoordinates, classesFile, project);
              if (sourceJar == null) {
                try {
                  sourceJar = new File(FileUtil.loadFile(sourcesLocationFile));
                  FileUtil.delete(sourcesLocationFile);
                }
                catch (IOException e) {
                  GradleLog.LOG.warn(e);
                }
              }
              File finalSourceJar = sourceJar;
              ApplicationManager.getApplication().invokeLater(() -> {
                final Set<Library> libraries = new HashSet<>();
                for (LibraryOrderEntry orderEntry : orderEntries) {
                  ContainerUtil.addIfNotNull(libraries, orderEntry.getLibrary());
                }
                if (finalSourceJar != null) {
                  attachSourceJar(finalSourceJar, libraries);
                }
                resultWrapper.setDone();
              });
            }

            @Override
            public void onFailure() {
              resultWrapper.setRejected();
              String message = ("<html>Sources not found for: " + artifactCoordinates) + "</html>";
              NotificationData notification = new NotificationData(
                "Sources download failed", message, NotificationCategory.WARNING, NotificationSource.PROJECT_SYNC);
              ExternalSystemNotificationManager.getInstance(project).showNotification(GradleConstants.SYSTEM_ID, notification);
            }
          }, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false, userData);

        return resultWrapper;
      }
    });
  }

  @Nullable
  private static File getSourceFile(String artifactCoordinates, VirtualFile classesFile, Project project) {
    LibraryData data = new LibraryData(GradleConstants.SYSTEM_ID, artifactCoordinates);
    data.addPath(LibraryPathType.BINARY, VfsUtil.getLocalFile(classesFile).getPath());
    String serviceDirectory = GradleSettings.getInstance(project).getServiceDirectoryPath();
    File gradleUserHome =
      serviceDirectory != null ? new File(serviceDirectory) : new BuildLayoutParameters().getGradleUserHomeDir();
    attachSourcesAndJavadocFromGradleCacheIfNeeded(gradleUserHome, data);
    Iterator<String> iterator = data.getPaths(LibraryPathType.SOURCE).iterator();
    return iterator.hasNext() ? new File(iterator.next()) : null;
  }

  private static Map<LibraryOrderEntry, Module> getGradleModules(List<LibraryOrderEntry> libraryOrderEntries) {
    Map<LibraryOrderEntry, Module> result = new HashMap<>();
    for (LibraryOrderEntry entry : libraryOrderEntries) {
      if (entry.isModuleLevel()) continue;
      Module module = entry.getOwnerModule();
      if (ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) {
        result.put(entry, module);
      }
    }
    return result;
  }
}
