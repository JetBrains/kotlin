// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.integrations.maven;

import com.intellij.CommonBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.indices.MavenSearchIndex;
import org.jetbrains.idea.maven.indices.MavenIndex;
import org.jetbrains.idea.maven.indices.MavenIndicesManager;
import org.jetbrains.idea.maven.model.MavenRemoteRepository;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.event.HyperlinkEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.idea.maven.indices.MavenIndicesManager.IndexUpdatingState.IDLE;

public class MavenRepositoriesHolder {
  private static final String UNINDEXED_MAVEN_REPOSITORIES_NOTIFICATION_GROUP = "Unindexed maven repositories gradle detection";
  private static final Key<String> NOTIFICATION_KEY = Key.create(UNINDEXED_MAVEN_REPOSITORIES_NOTIFICATION_GROUP);
  private final Project myProject;

  private volatile Set<MavenRemoteRepository> myRemoteRepositories;
  private volatile Set<String> myNotIndexedUrls;

  public MavenRepositoriesHolder(Project project) {
    myProject = project;
    myRemoteRepositories = Collections.emptySet();
    myNotIndexedUrls = Collections.emptySet();
  }

  public void updateNotIndexedUrls(List<String> repositories) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    if (repositories.isEmpty()) {
      myNotIndexedUrls = Collections.emptySet();
      ExternalSystemNotificationManager.getInstance(myProject).clearNotifications(
        UNINDEXED_MAVEN_REPOSITORIES_NOTIFICATION_GROUP, NotificationSource.PROJECT_SYNC, GradleConstants.SYSTEM_ID);
    }
    else {
      myNotIndexedUrls = new HashSet<>(repositories);
    }
  }

  public void checkNotIndexedRepositories() {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    ExternalSystemNotificationManager notificationManager = ExternalSystemNotificationManager.getInstance(myProject);
    if (ContainerUtil.isEmpty(myNotIndexedUrls)) {
      return;
    }

    if (notificationManager.isNotificationActive(NOTIFICATION_KEY)) return;

    final MavenIndicesManager indicesManager = MavenIndicesManager.getInstance();
    for (MavenSearchIndex index : indicesManager.getIndices()) {
      if (indicesManager.getUpdatingState(index) != IDLE) return;
    }

    final NotificationData notificationData = new NotificationData(
      GradleBundle.message("gradle.integrations.maven.notification.not_updated_repository.title"),
      "\n<br>" + GradleBundle.message("gradle.integrations.maven.notification.not_updated_repository.text"),
      NotificationCategory.INFO,
      NotificationSource.PROJECT_SYNC);
    notificationData.setBalloonNotification(true);
    notificationData.setBalloonGroup(UNINDEXED_MAVEN_REPOSITORIES_NOTIFICATION_GROUP);
    notificationData.setListener("#update", new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        List<MavenIndex> notIndexed =
          ContainerUtil.filter(indicesManager.getIndices(), index -> isNotIndexed(index.getRepositoryPathOrUrl()));
        indicesManager.scheduleUpdate(myProject, notIndexed).onSuccess(aVoid -> {
          if (myNotIndexedUrls.isEmpty()) return;
          for (MavenSearchIndex index : notIndexed) {
            if (index.getUpdateTimestamp() != -1 || index.getFailureMessage() != null) {
              myNotIndexedUrls.remove(index.getRepositoryPathOrUrl());
            }
          }
        });
        notification.hideBalloon();
      }
    });

    notificationData.setListener("#disable", new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        final int result =
          Messages.showYesNoDialog(myProject,
                                   "Notification will be disabled for all projects.\n\n" +
                                   "Settings | Appearance & Behavior | Notifications | " +
                                   UNINDEXED_MAVEN_REPOSITORIES_NOTIFICATION_GROUP +
                                   "\ncan be used to configure the notification.",
                                   "Unindexed Maven Repositories Gradle Detection",
                                   "Disable Notification", CommonBundle.getCancelButtonText(),
                                   Messages.getWarningIcon());
        if (result == Messages.YES) {
          NotificationsConfigurationImpl.getInstanceImpl().changeSettings(UNINDEXED_MAVEN_REPOSITORIES_NOTIFICATION_GROUP,
                                                                          NotificationDisplayType.NONE, false, false);

          notification.hideBalloon();
        }
      }
    });

    notificationManager.showNotification(GradleConstants.SYSTEM_ID, notificationData, NOTIFICATION_KEY);
  }

  public static MavenRepositoriesHolder getInstance(@NotNull Project p) {
    return ServiceManager.getService(p, MavenRepositoriesHolder.class);
  }

  public void update(Set<MavenRemoteRepository> remoteRepositories) {
    myRemoteRepositories = new HashSet<>(remoteRepositories);
  }

  public Set<MavenRemoteRepository> getRemoteRepositories() {
    return Collections.unmodifiableSet(myRemoteRepositories);
  }

  public boolean contains(String url) {
    final String pathOrUrl = MavenIndex.normalizePathOrUrl(url);
    for (MavenRemoteRepository repository : myRemoteRepositories) {
      if (MavenIndex.normalizePathOrUrl(repository.getUrl()).equals(pathOrUrl)) return true;
    }
    return false;
  }

  public boolean isNotIndexed(String url) {
    final String pathOrUrl = MavenIndex.normalizePathOrUrl(url);
    for (String repository : myNotIndexedUrls) {
      if (MavenIndex.normalizePathOrUrl(repository).equals(pathOrUrl)) return true;
    }
    return false;
  }
}
