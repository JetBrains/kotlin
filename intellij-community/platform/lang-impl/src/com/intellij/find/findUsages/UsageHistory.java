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
package com.intellij.find.findUsages;

import com.intellij.usages.ConfigurableUsageTarget;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.hash.EqualityPolicy;
import com.intellij.util.containers.hash.LinkedHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class UsageHistory {
  // the last element is the most recent
  @SuppressWarnings("unchecked")
  private final Map<ConfigurableUsageTarget, String> myHistory = new LinkedHashMap<ConfigurableUsageTarget, String>((EqualityPolicy<ConfigurableUsageTarget>)EqualityPolicy.IDENTITY) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<ConfigurableUsageTarget, String> eldest) {
      // todo configure history depth limit
      return size() > 15;
    }
  };

  public void add(@NotNull ConfigurableUsageTarget usageTarget) {
    final String descriptiveName = usageTarget.getLongDescriptiveName();
    synchronized (myHistory) {
      final Set<Map.Entry<ConfigurableUsageTarget, String>> entries = myHistory.entrySet();
      entries.removeIf(entry -> entry.getValue().equals(descriptiveName));
      myHistory.put(usageTarget, descriptiveName);
    }
  }

  @NotNull
  public List<ConfigurableUsageTarget> getAll() {
    synchronized (myHistory) {
      List<ConfigurableUsageTarget> result = ContainerUtil.newArrayList();
      final Set<ConfigurableUsageTarget> entries = myHistory.keySet();
      for (Iterator<ConfigurableUsageTarget> iterator = entries.iterator(); iterator.hasNext(); ) {
        final ConfigurableUsageTarget target = iterator.next();
        if (!target.isValid()) {
          iterator.remove();
        } else {
          result.add(target);
        }
      }
      return result;
    }
  }
}
