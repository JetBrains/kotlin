// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.util.messages.Topic;

/**
 * @author yole
 */
public interface CompletionPhaseListener {
  void completionPhaseChanged(boolean isCompletionRunning);

  Topic<CompletionPhaseListener> TOPIC = new Topic<>("CompletionPhaseListener", CompletionPhaseListener.class);
}
