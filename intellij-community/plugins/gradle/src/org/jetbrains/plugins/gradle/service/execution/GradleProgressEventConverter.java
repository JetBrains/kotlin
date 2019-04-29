/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.build.events.EventResult;
import com.intellij.build.events.impl.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent;
import org.gradle.tooling.events.*;
import org.gradle.tooling.events.internal.DefaultOperationDescriptor;
import org.gradle.tooling.events.task.TaskProgressEvent;
import org.gradle.tooling.events.task.TaskSuccessResult;
import org.gradle.tooling.internal.protocol.events.InternalOperationDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class GradleProgressEventConverter {

  static EventId getEventId(@NotNull ProgressEvent event, @NotNull String operationId) {
    OperationDescriptor descriptor = event.getDescriptor();
    InternalOperationDescriptor internalDescriptor = descriptor instanceof DefaultOperationDescriptor ?
                                                     ((DefaultOperationDescriptor)descriptor).getInternalOperationDescriptor() : null;
    String eventId = internalDescriptor == null ?
                     operationId + descriptor.getDisplayName() : operationId + internalDescriptor.getId().toString();
    String parentEventId = descriptor.getParent() == null ? null :
                           internalDescriptor == null
                           ? operationId + descriptor.getParent().getDisplayName()
                           : operationId + internalDescriptor.getParentId().toString();
    return new EventId(eventId, parentEventId);
  }

  @NotNull
  public static ExternalSystemTaskNotificationEvent convert(@NotNull ExternalSystemTaskId id,
                                                            @NotNull ProgressEvent event,
                                                            @NotNull String operationId) {
    return convert(id, event, getEventId(event, operationId));
  }

  @NotNull
  public static ExternalSystemTaskNotificationEvent convert(@NotNull ExternalSystemTaskId id,
                                                            @NotNull ProgressEvent event,
                                                            @NotNull EventId eventId) {
    final String description = event.getDescriptor().getName();

    if (event instanceof StartEvent) {
      return new ExternalSystemBuildEvent(
        id, new StartEventImpl(eventId.id, eventId.parentId, event.getEventTime(), description));
    }
    else if (event instanceof StatusEvent) {
      StatusEvent statusEvent = (StatusEvent)event;
      return new ExternalSystemBuildEvent(id, new ProgressBuildEventImpl(
        eventId.id, eventId.parentId, event.getEventTime(), description, statusEvent.getTotal(), statusEvent.getProgress(),
        statusEvent.getUnit()));
    }
    else if (event instanceof FinishEvent) {
      return new ExternalSystemBuildEvent(
        id,
        new FinishEventImpl(eventId.id, eventId.parentId, event.getEventTime(), description, convert(((FinishEvent)event).getResult())));
    }
    else if (event instanceof TaskProgressEvent) {
      return new ExternalSystemBuildEvent(
        id, new ProgressBuildEventImpl(eventId.id, eventId.parentId, event.getEventTime(), description, -1, -1, ""));
    }
    else {
      return new ExternalSystemTaskNotificationEvent(id, description);
    }
  }

  @NotNull
  public static ExternalSystemTaskNotificationEvent convert(ExternalSystemTaskId id, ProgressEvent event) {
    return convert(id, event, "");
  }

  @NotNull
  private static EventResult convert(OperationResult operationResult) {
    if (operationResult instanceof FailureResult) {
      return new FailureResultImpl(null, null);
    }
    else if (operationResult instanceof SkippedResult) {
      return new SkippedResultImpl();
    }
    else {
      final boolean isUpToDate = operationResult instanceof TaskSuccessResult && ((TaskSuccessResult)operationResult).isUpToDate();
      return new SuccessResultImpl(isUpToDate);
    }
  }

  static ExternalSystemTaskNotificationEvent createProgressBuildEvent(@NotNull ExternalSystemTaskId taskId,
                                                                      @NotNull Object id,
                                                                      @NotNull ProgressEvent event) {
    long total = -1;
    long progress = -1;
    String unit = "";
    String operationName = event.getDescriptor().getName();
    if (operationName.startsWith("Download ")) {
      String path = operationName.substring("Download ".length());
      operationName = "Download " + getFileName(path);
    }
    if(event instanceof StatusEvent) {
      total = ((StatusEvent)event).getTotal();
      progress = ((StatusEvent)event).getProgress();
      unit = ((StatusEvent)event).getUnit();
    }
    return new ExternalSystemBuildEvent(
      taskId, new ProgressBuildEventImpl(id, null, event.getEventTime(), operationName + "...", total, progress, unit));
  }

  @NotNull
  private static String getFileName(String path) {
    int index = path.lastIndexOf('/');
    if (index > 0 && index < path.length()) {
      String fileName = path.substring(index + 1);
      if (!fileName.isEmpty()) return fileName;
    }
    return path;
  }

  static class EventId {
    Object id;
    Object parentId;

    EventId(Object id, Object parentId) {
      this.id = id;
      this.parentId = parentId;
    }
  }
}
