// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server;

import com.intellij.util.messages.Topic;

import java.util.EventListener;

public interface CustomBuilderMessageHandler extends EventListener {
  /**
   * Custom builder message. Project level.
   */
  Topic<CustomBuilderMessageHandler> TOPIC = new Topic<>(CustomBuilderMessageHandler.class, Topic.BroadcastDirection.NONE);

  void messageReceived(String builderId, String messageType, String messageText);
}
