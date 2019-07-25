/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.notification.NotificationType;
import com.intellij.util.ui.MessageCategory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public enum NotificationCategory {
  ERROR(MessageCategory.ERROR),
  INFO(MessageCategory.INFORMATION),
  SIMPLE(MessageCategory.SIMPLE),
  WARNING(MessageCategory.WARNING);

  private final int myValue;

  NotificationCategory(int value) {
    myValue = value;
  }

  public int getMessageCategory() {
    return myValue;
  }

  @NotNull
  public NotificationType getNotificationType() {
    return convert(this);
  }

  public static NotificationType convert(NotificationCategory notificationCategory) {
    switch (notificationCategory) {
      case ERROR:
        return NotificationType.ERROR;
      case INFO:
        return NotificationType.INFORMATION;
      case SIMPLE:
        return NotificationType.INFORMATION;
      case WARNING:
        return NotificationType.WARNING;
      default:
        return NotificationType.INFORMATION;
    }
  }

  public static NotificationCategory convert(NotificationType notificationType) {
    switch (notificationType) {
      case INFORMATION:
        return INFO;
      case WARNING:
        return WARNING;
      case ERROR:
        return ERROR;
      default:
        return SIMPLE;
    }
  }

  public static NotificationCategory convert(int type) {
    switch (type) {
      case MessageCategory.ERROR:
        return ERROR;
      case MessageCategory.WARNING:
        return WARNING;
      case MessageCategory.INFORMATION:
        return INFO;
      case MessageCategory.STATISTICS:
        return INFO;
      case MessageCategory.SIMPLE:
        return SIMPLE;
      case MessageCategory.NOTE:
        return INFO;
      default:
        return SIMPLE;
    }
  }
}
