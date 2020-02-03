// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.stream.Stream;

public interface SharedIndexLoader {
  ExtensionPointName<SharedIndexLoader> EP_NAME = ExtensionPointName.create("com.intellij.sharedIndexLoader");

  boolean interruptLoading();



  @Nullable
  SharedIndexLoader.IndexStream loadIndex(@NotNull String chunkName);

  interface IndexStream {
    @NotNull
    String getChunkName();

    long getTimeStamp();

    @NotNull
    Stream<Pair<String, InputStream>> iterateIndexContent();

    void interrupt();

    boolean isDone();
  }
}
