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

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.HashMap;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class MessageCounter {

  private final Map<ProjectSystemId, Map<String/* group */, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>>>
    map = new HashMap<>();

  public synchronized void increment(@NotNull String groupName,
                                     @NotNull NotificationSource source,
                                     @NotNull NotificationCategory category,
                                     @NotNull ProjectSystemId projectSystemId) {

    final TObjectIntHashMap<NotificationCategory> counter =
      ContainerUtil.getOrCreate(
        ContainerUtil.getOrCreate(
          ContainerUtil.getOrCreate(
            map,
            projectSystemId,
            ContainerUtil.newHashMap()),
          groupName,
          ContainerUtil.newHashMap()
        ),
        source,
        new MyTObjectIntHashMap<>()
      );
    if (!counter.increment(category)) counter.put(category, 1);
  }

  public synchronized void remove(@Nullable final String groupName,
                                  @NotNull final NotificationSource notificationSource,
                                  @NotNull final ProjectSystemId projectSystemId) {
    final Map<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>> groupMap =
      ContainerUtil.getOrCreate(
        map,
        projectSystemId,
        ContainerUtil.newHashMap());
    if (groupName != null) {
      final TObjectIntHashMap<NotificationCategory> counter = ContainerUtil.getOrCreate(
        ContainerUtil.getOrCreate(
          groupMap,
          groupName,
          ContainerUtil.newHashMap()
        ),
        notificationSource,
        new MyTObjectIntHashMap<>()
      );
      counter.clear();
    }
    else {
      for (Map<NotificationSource, TObjectIntHashMap<NotificationCategory>> sourceMap : groupMap.values()) {
        sourceMap.remove(notificationSource);
      }
    }
  }

  public synchronized int getCount(@Nullable final String groupName,
                                   @NotNull final NotificationSource notificationSource,
                                   @Nullable final NotificationCategory notificationCategory,
                                   @NotNull final ProjectSystemId projectSystemId) {
    int count = 0;
    final Map<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>> groupMap = ContainerUtil.getOrElse(
      map,
      projectSystemId,
      Collections.emptyMap());

    for (Map.Entry<String, Map<NotificationSource, TObjectIntHashMap<NotificationCategory>>> entry : groupMap.entrySet()) {
      if (groupName == null || groupName.equals(entry.getKey())) {
        final TObjectIntHashMap<NotificationCategory> counter = entry.getValue().get(notificationSource);
        if (counter == null) continue;

        if (notificationCategory == null) {
          for (int aCount : counter.getValues()) {
            count += aCount;
          }
        }
        else {
          count += counter.get(notificationCategory);
        }
      }
    }

    return count;
  }

  @Override
  public String toString() {
    return "MessageCounter{" +
           "map=" + map +
           '}';
  }

  private static class MyTObjectIntHashMap<K> extends TObjectIntHashMap<K> {
    @Override
    public String toString() {
      return Arrays.toString(_set);
    }
  }
}


