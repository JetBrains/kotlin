// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.impl;

import com.intellij.util.messages.Topic;

public interface LineStatusTrackerSettingListener {
  Topic<LineStatusTrackerSettingListener> TOPIC = new Topic<>(LineStatusTrackerSettingListener.class, Topic.BroadcastDirection.TO_DIRECT_CHILDREN);

  void settingsUpdated();
}
