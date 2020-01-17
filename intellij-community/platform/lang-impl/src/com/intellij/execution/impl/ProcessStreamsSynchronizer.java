// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

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
 * Please note that ordering of data is guaranteed only in a stream (stdout/stderr/system),
 * no ordering guarantees are provided for data read from different streams. To order stdout/stderr messages,
 * stdout and stderr streams should be merged, see `run.processes.with.redirectedErrorStream` registry key.
 */
class ProcessStreamsSynchronizer {

  /**
   * Timeout to wait for the same stream data.<p>
   * Should be greater than {@code SleepingPolicy.NON_BLOCKING.getTimeToSleep(false)}
   *
   * @see com.intellij.util.io.BaseDataReader.SleepingPolicy#NON_BLOCKING
   */
  static final long AWAIT_SAME_STREAM_TEXT_NANO = TimeUnit.MILLISECONDS.toNanos(10);

  private final Object myLock = new Object();
  private final List<Chunk> myPendingChunks = new ArrayList<>();
  private final Alarm myAlarm;
  private final boolean isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode();
  private boolean myFlushedChunksEndWithNewline = true;
  private ProcessOutputType myLastFlushedChunkBaseOutputType = null;
  private long myLastFlushedChunkCreatedNanoTime = 0;
  private int myReschedules = 0;

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
      ProcessOutputType baseOutputType = outputType.getBaseOutputType();
      if (ProcessOutputType.SYSTEM.equals(baseOutputType)) {
        handleSystemOutput(flushRunnable, nowNano);
        return;
      }
      Boolean textEndsWithNewline = text.isEmpty() ? null : StringUtil.endsWithChar(text, '\n');
      if ((myLastFlushedChunkBaseOutputType == null || baseOutputType.equals(myLastFlushedChunkBaseOutputType))) {
        boolean newlineAdded = !myFlushedChunksEndWithNewline && Boolean.TRUE.equals(textEndsWithNewline);
        if (textEndsWithNewline != null) {
          myFlushedChunksEndWithNewline = textEndsWithNewline;
        }
        myLastFlushedChunkBaseOutputType = baseOutputType;
        myLastFlushedChunkCreatedNanoTime = nowNano;
        flushRunnable.run();
        if (newlineAdded && myPendingChunks.size() > 0
            && myPendingChunks.get(0).getNanoTimePassedSinceLastFlushedChunk(nowNano) >= AWAIT_SAME_STREAM_TEXT_NANO) {
          processPendingChunks(nowNano);
        }
        return;
      }
      Chunk chunk = new Chunk(textEndsWithNewline, baseOutputType, nowNano, myLastFlushedChunkCreatedNanoTime, flushRunnable);
      myPendingChunks.add(chunk);
      if (!isProcessingScheduled()) {
        long delayNano = AWAIT_SAME_STREAM_TEXT_NANO - chunk.getNanoTimePassedSinceLastFlushedChunk(nowNano);
        if (delayNano <= 0) {
          processPendingChunks(nowNano);
        }
        else {
          myReschedules++;
          scheduleProcessPendingChunks(delayNano);
        }
      }
    }
  }

  boolean isProcessingScheduled() {
    return !myAlarm.isEmpty();
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
    myPendingChunks.add(new Chunk(null, first.myBaseOutputType, nowNano, myLastFlushedChunkCreatedNanoTime, flushRunnable));
  }

  final void processPendingChunks(long nowNano) {
    synchronized (myLock) {
      if (myPendingChunks.isEmpty()) {
        // There could be no pending chunks in the following case:
        // When pending chunks were processed for the first time, another stream didn't end with a newline => the chunks were re-scheduled.
        // Then, another stream got a newline => the pending chunks were processed immediately.
        myReschedules = 0;
        return;
      }
      // All pending chunks should have the same base output type (`chunk.myBaseOutputType`).
      Chunk eldestChunk = myPendingChunks.get(0);
      long awaitNano = eldestChunk.getNanoTimePassedSinceLastFlushedChunk(nowNano);
      if ((awaitNano >= AWAIT_SAME_STREAM_TEXT_NANO && myFlushedChunksEndWithNewline) || myReschedules > (isUnitTestMode ? 10 : 1)) {
        flushAllPendingChunks();
      }
      if (!myPendingChunks.isEmpty()) {
        myReschedules++;
        scheduleProcessPendingChunks(AWAIT_SAME_STREAM_TEXT_NANO);
      }
      else {
        myReschedules = 0;
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
    if (chunk.myTextEndsWithNewline != null) {
      myFlushedChunksEndWithNewline = chunk.myTextEndsWithNewline;
    }
    myLastFlushedChunkBaseOutputType = chunk.myBaseOutputType;
    myLastFlushedChunkCreatedNanoTime = chunk.myCreatedNanoTime;
    chunk.myFlushRunnable.run();
  }

  @TestOnly
  void waitForAllFlushed() {
    while (true) {
      synchronized (myLock) {
        if (myPendingChunks.isEmpty()) {
          return;
        }
        if (myAlarm.isEmpty()) {
          throw new RuntimeException("No requests scheduled");
        }
      }
      try {
        myAlarm.waitForAllExecuted(10, TimeUnit.SECONDS);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class Chunk {
    private final @Nullable Boolean myTextEndsWithNewline;
    private final ProcessOutputType myBaseOutputType;
    private final long myCreatedNanoTime;
    private final long myPrevFlushedChunkCreatedNanoTime;
    private final Runnable myFlushRunnable;

    private Chunk(@Nullable Boolean textEndsWithNewline,
                  @NotNull ProcessOutputType baseOutputType,
                  long createdNanoTime,
                  long prevFlushedChunkCreatedNanoTime,
                  @NotNull Runnable flushRunnable) {
      myTextEndsWithNewline = textEndsWithNewline;
      myBaseOutputType = baseOutputType;
      myCreatedNanoTime = createdNanoTime;
      myPrevFlushedChunkCreatedNanoTime = prevFlushedChunkCreatedNanoTime;
      myFlushRunnable = flushRunnable;
    }

    long getNanoTimePassedSinceLastFlushedChunk(long nowNanoTime) {
      return myPrevFlushedChunkCreatedNanoTime > 0 ? nowNanoTime - myPrevFlushedChunkCreatedNanoTime
                                                   : nowNanoTime - myCreatedNanoTime;
    }

    @Override
    public String toString() {
      return "ends-with-newline='" + myTextEndsWithNewline + '\'' + ", outputType=" + myBaseOutputType;
    }
  }
}
