// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
public interface BuildEventDispatcher extends Appendable, Closeable, BuildProgressListener {
  @Override
  default BuildEventDispatcher append(CharSequence csq) {return this;}

  @Override
  default BuildEventDispatcher append(CharSequence csq, int start, int end) {return this;}

  @Override
  default BuildEventDispatcher append(char c) {return this;}

  /**
   * Registers handler which is invoked once the build process is finished and the build messages are dispatched.
   *
   * @throws UnsupportedOperationException if underlying implementation doesn't support {@link #invokeOnCompletion}.
   */
  default void invokeOnCompletion(@NotNull Consumer<Throwable> consumer) {
    throw new UnsupportedOperationException("invokeOnCompletion is not supported by this BuildEventDispatcher");
  }

  @Override
  default void close() {}

  default void setStdOut(boolean stdOut) {}
}
