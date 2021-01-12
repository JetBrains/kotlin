// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.findUsages;

import com.intellij.usages.ConfigurableUsageTarget;
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
      List<ConfigurableUsageTarget> result = new ArrayList<>();
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
