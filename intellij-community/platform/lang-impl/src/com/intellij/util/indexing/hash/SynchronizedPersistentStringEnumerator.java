// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.util.io.PersistentStringEnumerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public class SynchronizedPersistentStringEnumerator extends PersistentStringEnumerator {
  public SynchronizedPersistentStringEnumerator(@NotNull Path file) throws IOException {
    super(file);
  }

  @Override
  public synchronized int enumerate(@Nullable String value) throws IOException {
    return super.enumerate(value);
  }

  @Override
  public synchronized int tryEnumerate(String name) throws IOException {
    return super.tryEnumerate(name);
  }

  @Override
  public synchronized @Nullable String valueOf(int idx) throws IOException {
    return super.valueOf(idx);
  }

  @Override
  public synchronized void close() throws IOException {
    super.close();
  }

  @Override
  public synchronized boolean isClosed() {
    return super.isClosed();
  }

  @Override
  public synchronized void force() {
    super.force();
  }

  @Override
  public synchronized boolean isDirty() {
    return super.isDirty();
  }

  @Override
  public synchronized boolean isCorrupted() {
    return super.isCorrupted();
  }
}
