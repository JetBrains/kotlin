// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;
import java.util.List;

public interface EditorTrackerListener extends EventListener{
  Topic<EditorTrackerListener> TOPIC =
    new Topic<>(EditorTrackerListener.class.getSimpleName(), EditorTrackerListener.class, Topic.BroadcastDirection.NONE);

  void activeEditorsChanged(@NotNull List<Editor> activeEditors);
}
