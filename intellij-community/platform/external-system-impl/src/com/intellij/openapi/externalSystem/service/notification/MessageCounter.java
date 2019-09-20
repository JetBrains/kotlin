// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.notification;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
            new java.util.HashMap<>()),
          groupName,
          new java.util.HashMap<>()
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
        new java.util.HashMap<>());
    if (groupName != null) {
      final TObjectIntHashMap<NotificationCategory> counter = ContainerUtil.getOrCreate(
        ContainerUtil.getOrCreate(
          groupMap,
          groupName,
          new java.util.HashMap<>()
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


