// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.startup;

import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.config.GradleResourceCompilerConfigurationGenerator;
import org.jetbrains.plugins.gradle.service.GradleBuildClasspathManager;
import org.jetbrains.plugins.gradle.service.project.GradleNotification;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder;
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.event.HyperlinkEvent;
import java.io.File;

/**
 * @author Vladislav.Soroka
 */
public class GradleJvmStartupActivity implements StartupActivity {

  @NonNls private static final String SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup";
  private static final String IMPORT_EVENT_DESCRIPTION = "import";
  private static final String DO_NOT_SHOW_EVENT_DESCRIPTION = "do.not.show";

  @Override
  public void runActivity(@NotNull final Project project) {
    configureBuildClasspath(project);
    showNotificationForUnlinkedGradleProject(project);
    final GradleResourceCompilerConfigurationGenerator
      buildConfigurationGenerator = new GradleResourceCompilerConfigurationGenerator(project);
    CompilerManager.getInstance(project).addBeforeTask(new CompileTask() {
      @Override
      public boolean execute(@NotNull CompileContext context) {
        ApplicationManager.getApplication().runReadAction(() -> buildConfigurationGenerator.generateBuildConfiguration(context));
        return true;
      }
    });
  }

  private static void configureBuildClasspath(@NotNull final Project project) {
    GradleBuildClasspathManager.getInstance(project).reload();
  }

  private static void showNotificationForUnlinkedGradleProject(@NotNull final Project project) {
    if (!PropertiesComponent.getInstance(project).getBoolean(SHOW_UNLINKED_GRADLE_POPUP, true)
        || !GradleSettings.getInstance(project).getLinkedProjectsSettings().isEmpty()
        || project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == Boolean.TRUE
        || project.getBaseDir() == null) {
      return;
    }

    String baseDir = project.getBaseDir().getPath();
    String gradleGroovyDslFile = baseDir + '/' + GradleConstants.DEFAULT_SCRIPT_NAME;
    String kotlinDslGradleFile = baseDir + '/' + GradleConstants.KOTLIN_DSL_SCRIPT_NAME;
    File gradleFile = FileUtil.findFirstThatExist(gradleGroovyDslFile, kotlinDslGradleFile);

    if (gradleFile != null) {
      String message = String.format("%s<br>\n%s",
                                     GradleBundle.message("gradle.notifications.unlinked.project.found.msg", IMPORT_EVENT_DESCRIPTION),
                                     GradleBundle.message("gradle.notifications.do.not.show"));

      GradleNotification.getInstance(project).showBalloon(
        GradleBundle.message("gradle.notifications.unlinked.project.found.title"),
        message, NotificationType.INFORMATION, new NotificationListener.Adapter() {
          @Override
          protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
            notification.expire();
            if (IMPORT_EVENT_DESCRIPTION.equals(e.getDescription())) {
              final ProjectDataManager projectDataManager = ServiceManager.getService(ProjectDataManager.class);
              GradleProjectImportBuilder gradleProjectImportBuilder = new GradleProjectImportBuilder(projectDataManager);
              final GradleProjectImportProvider gradleProjectImportProvider = new GradleProjectImportProvider(gradleProjectImportBuilder);
              AddModuleWizard wizard = new AddModuleWizard(project, gradleFile.getPath(), gradleProjectImportProvider);
              if ((wizard.getStepCount() <= 0 || wizard.showAndGet())) {
                ImportModuleAction.createFromWizard(project, wizard);
              }
            }
            else if (DO_NOT_SHOW_EVENT_DESCRIPTION.equals(e.getDescription())) {
              PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true);
            }
          }
        }
      );
    }
  }
}
