// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.codeInsight;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.LightColors;
import org.gradle.util.GUtil;
import org.gradle.wrapper.WrapperConfiguration;
import org.gradle.wrapper.WrapperExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleUtil;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author Vladislav.Soroka
 */
public final class UseDistributionWithSourcesNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements
                                                                                                                          DumbAware {
  public static final Pattern GRADLE_SRC_DISTRIBUTION_PATTERN;
  private static final Logger LOG = Logger.getInstance(UseDistributionWithSourcesNotificationProvider.class);
  private static final Key<EditorNotificationPanel> KEY = Key.create("gradle.notifications.use.distribution.with.sources");
  private static final String ALL_ZIP_DISTRIBUTION_URI_SUFFIX = "-all.zip";

  static {
    GRADLE_SRC_DISTRIBUTION_PATTERN = Pattern.compile("https?\\\\?://services\\.gradle\\.org.*" + ALL_ZIP_DISTRIBUTION_URI_SUFFIX);
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
    try {
      if (GradleConstants.DEFAULT_SCRIPT_NAME.equals(file.getName()) ||
          GradleConstants.SETTINGS_FILE_NAME.equals(file.getName())) {

        final Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) return null;
        final String rootProjectPath = getRootProjectPath(module);
        if (rootProjectPath == null) return null;
        final GradleProjectSettings settings = GradleSettings.getInstance(module.getProject()).getLinkedProjectSettings(rootProjectPath);
        if (settings == null || settings.getDistributionType() != DistributionType.DEFAULT_WRAPPED) return null;
        if (settings.isDisableWrapperSourceDistributionNotification()) return null;
        if (!showUseDistributionWithSourcesTip(rootProjectPath)) return null;

        final EditorNotificationPanel panel = new EditorNotificationPanel(LightColors.SLIGHTLY_GREEN);
        panel.setText(GradleBundle.message("gradle.notifications.use.distribution.with.sources"));
        panel.createActionLabel(GradleBundle.message("gradle.notifications.hide.tip"), () -> {
          settings.setDisableWrapperSourceDistributionNotification(true);
          EditorNotifications.getInstance(module.getProject()).updateAllNotifications();
        });
        panel.createActionLabel(GradleBundle.message("gradle.notifications.apply.suggestion"), () -> {
          updateDefaultWrapperConfiguration(rootProjectPath);
          EditorNotifications.getInstance(module.getProject()).updateAllNotifications();
          ExternalSystemUtil.refreshProject(settings.getExternalProjectPath(),
                                            new ImportSpecBuilder(module.getProject(), GradleConstants.SYSTEM_ID)
                                              .use(ProgressExecutionMode.START_IN_FOREGROUND_ASYNC));
        });
        return panel;
      }
    }
    catch (ProcessCanceledException | IndexNotReadyException ignored) {
    }

    return null;
  }

  private static void updateDefaultWrapperConfiguration(@NotNull String linkedProjectPath) {
    try {
      final File wrapperPropertiesFile = GradleUtil.findDefaultWrapperPropertiesFile(linkedProjectPath);
      if (wrapperPropertiesFile == null) return;
      final WrapperConfiguration wrapperConfiguration = GradleUtil.getWrapperConfiguration(linkedProjectPath);
      if (wrapperConfiguration == null) return;
      String currentDistributionUri = wrapperConfiguration.getDistribution().toString();
      if (StringUtil.endsWith(currentDistributionUri, ALL_ZIP_DISTRIBUTION_URI_SUFFIX)) return;

      final String distributionUri =
        currentDistributionUri.substring(0, currentDistributionUri.lastIndexOf('-')) + ALL_ZIP_DISTRIBUTION_URI_SUFFIX;

      wrapperConfiguration.setDistribution(new URI(distributionUri));
      Properties wrapperProperties = new Properties();
      wrapperProperties.setProperty(WrapperExecutor.DISTRIBUTION_URL_PROPERTY, wrapperConfiguration.getDistribution().toString());
      wrapperProperties.setProperty(WrapperExecutor.DISTRIBUTION_BASE_PROPERTY, wrapperConfiguration.getDistributionBase());
      wrapperProperties.setProperty(WrapperExecutor.DISTRIBUTION_PATH_PROPERTY, wrapperConfiguration.getDistributionPath());
      wrapperProperties.setProperty(WrapperExecutor.ZIP_STORE_BASE_PROPERTY, wrapperConfiguration.getZipBase());
      wrapperProperties.setProperty(WrapperExecutor.ZIP_STORE_PATH_PROPERTY, wrapperConfiguration.getZipPath());
      GUtil.saveProperties(wrapperProperties, new File(wrapperPropertiesFile.getPath()));
      LocalFileSystem.getInstance().refreshIoFiles(Collections.singletonList(wrapperPropertiesFile));
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private static boolean showUseDistributionWithSourcesTip(String linkedProjectPath) {
    WrapperConfiguration wrapperConfiguration = GradleUtil.getWrapperConfiguration(linkedProjectPath);
    // currently only wrapped distribution takes into account
    if (wrapperConfiguration == null) return true;
    String distributionUri = wrapperConfiguration.getDistribution().toString();
    try {
      String host = new URI(distributionUri).getHost();
      return host != null && host.endsWith("gradle.org") && !GRADLE_SRC_DISTRIBUTION_PATTERN.matcher(distributionUri).matches();
    }
    catch (URISyntaxException ignore) {
    }
    return false;
  }

  @Nullable
  private static String getRootProjectPath(@NotNull Module module) {
    ExternalSystemModulePropertyManager modulePropertyManager = ExternalSystemModulePropertyManager.getInstance(module);
    String externalSystemId = modulePropertyManager.getExternalSystemId();
    if (externalSystemId == null || !GradleConstants.SYSTEM_ID.toString().equals(externalSystemId)) {
      return null;
    }

    String path = modulePropertyManager.getRootProjectPath();
    return StringUtil.isEmpty(path) ? null : path;
  }
}
