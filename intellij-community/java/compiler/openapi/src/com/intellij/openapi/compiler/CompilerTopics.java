// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.compiler;

import com.intellij.util.messages.Topic;

public final class CompilerTopics {
  /**
   * Project level.
   */
  public static final Topic<CompilationStatusListener> COMPILATION_STATUS = new Topic<>(CompilationStatusListener.class, Topic.BroadcastDirection.NONE);

  private CompilerTopics() {
  }
}
