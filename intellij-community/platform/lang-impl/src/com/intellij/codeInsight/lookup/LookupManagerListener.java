// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.Nullable;

public interface LookupManagerListener {
  void activeLookupChanged(@Nullable Lookup oldLookup, @Nullable Lookup newLookup);

  Topic<LookupManagerListener> TOPIC = Topic.create("lookup manager listener", LookupManagerListener.class);
}
