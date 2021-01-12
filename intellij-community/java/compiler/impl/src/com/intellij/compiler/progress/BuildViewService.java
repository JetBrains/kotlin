// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.progress;

import com.intellij.compiler.impl.ExitStatus;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.ApiStatus;

/**
 * {@link BuildViewService} implementations are expected to visualize somehow compiler progress/messages for {@link CompilerTask}.
 *
 * @see CompilerTask
 */
@ApiStatus.Internal
public interface BuildViewService {
  void onStart(Object sessionId, long startCompilationStamp, Runnable restartWork, ProgressIndicator indicator);

  void onEnd(Object sessionId, ExitStatus exitStatus, long endCompilationStamp);

  void addMessage(Object sessionId, CompilerMessage message);

  void onProgressChange(Object sessionId, ProgressIndicator indicator);

  void registerCloseAction(Runnable onClose);
}
