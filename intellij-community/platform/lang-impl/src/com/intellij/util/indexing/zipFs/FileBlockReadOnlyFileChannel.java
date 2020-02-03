// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class FileBlockReadOnlyFileChannel extends FileChannel {
  @NotNull
  private final FileChannel myUnderlying;
  private final long myStartOffset;
  private final long myEndOffset;
  private final long mySize;

  // TODO duplicates?
  private volatile long myGlobalPosition;
  private volatile long myLocalPosition;

  public FileBlockReadOnlyFileChannel(@NotNull FileChannel underlying,
                                      long startOffset,
                                      long size) {
    myUnderlying = underlying;

    myStartOffset = startOffset;
    myEndOffset = startOffset + size;
    mySize = size;

    myGlobalPosition = myStartOffset;
    myLocalPosition = 0;
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    int read = read(dst, myLocalPosition);
    if (read != -1 && read != 0) {
      position(myLocalPosition + read);
    }
    return read;
  }

  @Override
  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    throw new IOException("Not implemented");
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    throw new IOException("Write operation is not supported");
  }

  @Override
  public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    throw new IOException("Write operation is not supported");
  }

  @Override
  public long position() {
    return myGlobalPosition - myStartOffset;
  }

  @Override
  public FileChannel position(long newPosition) {
    myGlobalPosition = myStartOffset + newPosition;
    myLocalPosition = newPosition;
    assert myGlobalPosition <= myEndOffset;
    assert myLocalPosition <= mySize;
    return this;
  }

  @Override
  public long size() {
    return mySize;
  }

  @Override
  public FileChannel truncate(long size){
    throw new UnsupportedOperationException();
  }

  @Override
  public void force(boolean metaData) {
    // do nothing
  }

  @Override
  public long transferTo(long position, long count, WritableByteChannel target) {
    throw new UnsupportedOperationException("Implement it!");
  }

  @Override
  public long transferFrom(ReadableByteChannel src, long position, long count) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int read(ByteBuffer dst, long position) throws IOException {
    long globalPosition = myStartOffset + position;
    if (position >= mySize) {
      return -1;
    }
    return myUnderlying.read(dst, globalPosition);
  }

  @Override
  public int write(ByteBuffer src, long position) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public MappedByteBuffer map(MapMode mode, long position, long size) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileLock lock(long position, long size, boolean shared) {
    return null;
  }

  @Override
  public FileLock tryLock(long position, long size, boolean shared) {
    return null;
  }

  @Override
  protected void implCloseChannel() {
    // do nothing
  }
}
