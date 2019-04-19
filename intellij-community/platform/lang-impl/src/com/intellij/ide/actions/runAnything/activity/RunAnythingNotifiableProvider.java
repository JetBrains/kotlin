// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject;

/**
 * Implement notifiable provider if you desire to run an arbitrary activity in the IDE, that may hasn't provide visual effects,
 * and show notification about it with optional ability to rollback the action as {@link #getRollbackAction(DataContext)}.
 *
 * @param <V> see {@link RunAnythingProvider}
 */
public abstract class RunAnythingNotifiableProvider<V> extends RunAnythingProviderBase<V> {
  private static final String RUN_ANYTHING_GROUP_ID = IdeBundle.message("run.anything.custom.activity.notification.group.id");

  /**
   * Runs an activity silently. After the exception is passed a notification is shown.
   *
   * @param dataContext 'Run Anything' data context
   * @return true if succeed, false is failed
   */
  protected abstract boolean run(@NotNull DataContext dataContext, @NotNull V value);

  /**
   * Creates rollback action for {@link #run(DataContext, Object)}
   *
   * @param dataContext 'Run Anything' data context
   * @return rollback action for {@link #run(DataContext, Object)}, null if isn't provided
   */
  @Nullable
  protected abstract Runnable getRollbackAction(@NotNull DataContext dataContext);

  /**
   * Creates post activity {@link Notification} title
   *
   * @param dataContext 'Run Anything' data context
   */
  @NotNull
  protected abstract String getNotificationTitle(@NotNull DataContext dataContext, @NotNull V value);

  /**
   * Creates post activity {@link Notification} content
   *
   * @param dataContext 'Run Anything' data context
   */
  @NotNull
  protected abstract String getNotificationContent(@NotNull DataContext dataContext, @NotNull V value);


  /**
   * Executes arbitrary activity in IDE and shows {@link Notification} with optional rollback action
   *
   * @param dataContext 'Run Anything' data context
   * @return true if succeed, false is failed
   */
  @Override
  public void execute(@NotNull DataContext dataContext, @NotNull V value) {
    if (run(dataContext, value)) {
      getNotificationCallback(dataContext, value).run();
    }
    else {
      Messages.showWarningDialog(fetchProject(dataContext),
                                 IdeBundle.message("run.anything.notification.warning.content", getCommand(value)),
                                 IdeBundle.message("run.anything.notification.warning.title"));
    }
  }

  private Runnable getNotificationCallback(@NotNull DataContext dataContext, @NotNull V value) {
    return () -> {
      Notification notification = new Notification(
        RUN_ANYTHING_GROUP_ID,
        AllIcons.Actions.Run_anything,
        getNotificationTitle(dataContext, value),
        null,
        getNotificationContent(dataContext, value),
        NotificationType.INFORMATION,
        null
      );

      Runnable rollbackAction = getRollbackAction(dataContext);
      if (rollbackAction != null) {
        AnAction action = new AnAction(IdeBundle.message("run.anything.custom.activity.rollback.action")) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            rollbackAction.run();
            notification.expire();
          }
        };
        notification.addAction(action);
      }

      Notifications.Bus.notify(notification);
    };
  }
}