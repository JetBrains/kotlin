// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import com.intellij.util.LineSeparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tries to increase probability that a continuous data printed to a stream does not interleave with data
 * from other streams when printed to a console.
 * <p>
 * For example, it increases probability to see a complete stack trace printed to stderr without some occasional
 * data from stdout inserted inside the stack trace.
 * <p>
 * Please note that ordering of data is guaranteed only in a stream (stdout/stderr/system), no ordering of data
 * read between different streams.
 */
class ProcessStreamsSynchronizer {

  private static final Logger LOG = Logger.getInstance(ProcessStreamsSynchronizer.class);
  /**
   * Timeout to wait for the same stream data.<p>
   * Should be greater than {@code SleepingPolicy.NON_BLOCKING.getTimeToSleep(false)}
   * @see com.intellij.util.io.BaseDataReader.SleepingPolicy#NON_BLOCKING
   */
  static final long AWAIT_SAME_STREAM_TEXT_NANO = TimeUnit.MILLISECONDS.toNanos(10);
  /**
   * A bit increased timeout to increase probability that stdout and stderr occupy different lines.
   */
  static final long AWAIT_NEW_LINE_NANO = TimeUnit.MILLISECONDS.toNanos(20);

  private final Object myLock = new Object();
  private final List<Chunk> myPendingChunks = new ArrayList<>();
  private boolean myFlushedChunksEndWithNewLine = true;
  private Chunk myLastFlushedChunk = null;
  private final Alarm myAlarm;

  ProcessStreamsSynchronizer(@NotNull Disposable parentDisposable) {
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
    Disposer.register(parentDisposable, new Disposable() {
      @Override
      public void dispose() {
        synchronized (myLock) {
          flushAllPendingChunks();
        }
      }
    });
  }

  final void doWhenStreamsSynchronized(@NotNull String text, @NotNull ProcessOutputType outputType, @NotNull Runnable flushRunnable) {
    long nowNano = getNanoTime();
    synchronized (myLock) {
      if (ProcessOutputType.SYSTEM.equals(outputType.getBaseOutputType())) {
        handleSystemOutput(flushRunnable, nowNano);
        return;
      }
      Chunk chunk = new Chunk(text, outputType, nowNano, myLastFlushedChunk, flushRunnable);
      if ((myLastFlushedChunk == null || outputType.getBaseOutputType().equals(myLastFlushedChunk.myOutputType.getBaseOutputType()))) {
        flush(chunk);
        return;
      }
      myPendingChunks.add(chunk);
      if (myAlarm.isEmpty()) {
        long timeoutNano = myLastFlushedChunk.myCreatedNanoTime + AWAIT_SAME_STREAM_TEXT_NANO - nowNano;
        if (timeoutNano <= 0) {
          processPendingChunks(nowNano);
        }
        else {
          scheduleProcessPendingChunks(timeoutNano);
        }
      }
    }
  }

  long getNanoTime() {
    return System.nanoTime();
  }

  void scheduleProcessPendingChunks(long delayNano) {
    myAlarm.addRequest(() -> processPendingChunks(getNanoTime()), TimeUnit.NANOSECONDS.toMillis(delayNano));
  }

  /*
   * System outputs shouldn't be merged with each other. Otherwise, for short-living processes the first (command line) and
   * the last ("Process finished with exit code") system messages might be wrongly merged leading to the following output:
   * "command line"
   * "Process finished with exit code"
   * stdout/stderr
   *
   * System outputs should be simply flushed after all pending chunks.
   */
  private void handleSystemOutput(@NotNull Runnable flushRunnable, long nowNano) {
    if (myPendingChunks.isEmpty()) {
      flushRunnable.run();
      return;
    }
    Chunk first = myPendingChunks.get(0);
    // flush after the last pending chunk
    myPendingChunks.add(new Chunk("", first.myOutputType.getBaseOutputType(), nowNano, null, flushRunnable));
  }

  final void processPendingChunks(long nowNano) {
    synchronized (myLock) {
      if (myPendingChunks.isEmpty()) {
        LOG.error("No pending chunks unexpectedly");
        return;
      }
      // All pending chunks should have the same base output type (`chunk.myOutputType.getBaseOutputType()`).
      Chunk eldestChunk = myPendingChunks.get(0);
      long awaitNano = nowNano - eldestChunk.myCreatedNanoTime;
      if (eldestChunk.myPrevFlushedChunk != null) {
        awaitNano = nowNano - eldestChunk.myPrevFlushedChunk.myCreatedNanoTime;
      }
      if (awaitNano >= AWAIT_SAME_STREAM_TEXT_NANO && myFlushedChunksEndWithNewLine || awaitNano >= AWAIT_NEW_LINE_NANO) {
        flushAllPendingChunks();
      }
      myAlarm.cancelAllRequests();
      if (!myPendingChunks.isEmpty()) {
        scheduleProcessPendingChunks(AWAIT_SAME_STREAM_TEXT_NANO);
      }
    }
  }

  private void flushAllPendingChunks() {
    for (Chunk chunk : myPendingChunks) {
      flush(chunk);
    }
    myPendingChunks.clear();
  }

  private void flush(@NotNull Chunk chunk) {
    if (!chunk.myText.isEmpty()) {
      myFlushedChunksEndWithNewLine = chunk.myText.endsWith(LineSeparator.LF.getSeparatorString());
    }
    myLastFlushedChunk = chunk;
    chunk.myFlushRunnable.run();
  }

  private static class Chunk {
    private final String myText;
    private final ProcessOutputType myOutputType;
    private final long myCreatedNanoTime;
    private final Chunk myPrevFlushedChunk;
    private final Runnable myFlushRunnable;

    private Chunk(@NotNull String text,
                  @NotNull ProcessOutputType outputType,
                  long createdNanoTime,
                  @Nullable Chunk prevFlushedChunk,
                  @NotNull Runnable flushRunnable) {
      myText = text;
      myOutputType = outputType;
      myCreatedNanoTime = createdNanoTime;
      myPrevFlushedChunk = prevFlushedChunk;
      myFlushRunnable = flushRunnable;
    }

    @Override
    public String toString() {
      return "text='" + myText + '\'' + ", outputType=" + myOutputType;
    }
  }
}
