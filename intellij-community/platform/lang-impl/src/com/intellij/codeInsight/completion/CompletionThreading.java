// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.FutureResult;
import com.intellij.util.concurrency.Semaphore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author peter
 */
interface CompletionThreading {

  Future<?> startThread(final ProgressIndicator progressIndicator, Runnable runnable);

  WeighingDelegate delegateWeighing(CompletionProgressIndicator indicator);
}

interface WeighingDelegate extends Consumer<CompletionResult> {
  void waitFor();
}

class SyncCompletion extends CompletionThreadingBase {
  private final List<CompletionResult> myBatchList = new ArrayList<>();

  @Override
  public Future<?> startThread(final ProgressIndicator progressIndicator, Runnable runnable) {
    ProgressManager.getInstance().runProcess(runnable, progressIndicator);

    FutureResult<Object> result = new FutureResult<>();
    result.set(true);
    return result;
  }

  @Override
  public WeighingDelegate delegateWeighing(final CompletionProgressIndicator indicator) {
    return new WeighingDelegate() {
      @Override
      public void waitFor() {
        indicator.addDelayedMiddleMatches();
      }

      @Override
      public void consume(CompletionResult result) {
        if (ourIsInBatchUpdate.get().booleanValue()) {
          myBatchList.add(result);
        } else {
          indicator.addItem(result);
        }
      }
    };
  }

  @Override
  protected void flushBatchResult(CompletionProgressIndicator indicator) {
    try {
      indicator.withSingleUpdate(() -> {
        for (CompletionResult result : myBatchList) {
          indicator.addItem(result);
        }
      });
    } finally {
      myBatchList.clear();
    }
  }
}

class AsyncCompletion extends CompletionThreadingBase {
  private static final Logger LOG = Logger.getInstance(AsyncCompletion.class);
  private final ArrayList<CompletionResult> myBatchList = new ArrayList<>();
  private final LinkedBlockingQueue<Computable<Boolean>> myQueue = new LinkedBlockingQueue<>();

  @Override
  public Future<?> startThread(final ProgressIndicator progressIndicator, final Runnable runnable) {
    final Semaphore startSemaphore = new Semaphore();
    startSemaphore.down();
    Future<?> future = ApplicationManager.getApplication().executeOnPooledThread(() -> ProgressManager.getInstance().runProcess(() -> {
      try {
        startSemaphore.up();
        ProgressManager.checkCanceled();
        runnable.run();
      }
      catch (ProcessCanceledException ignored) {
      }
    }, progressIndicator));
    startSemaphore.waitFor();
    return future;
  }

  @Override
  public WeighingDelegate delegateWeighing(final CompletionProgressIndicator indicator) {

    class WeighItems implements Runnable {
      @Override
      public void run() {
        try {
          while (true) {
            Computable<Boolean> next = myQueue.poll(30, TimeUnit.MILLISECONDS);
            if (next != null && !next.compute()) {
              tryReadOrCancel(indicator, () -> indicator.addDelayedMiddleMatches());
              return;
            }
            indicator.checkCanceled();
          }
        } catch (InterruptedException e) {
          LOG.error(e);
        }
      }
    }

    final Future<?> future = startThread(ProgressWrapper.wrap(indicator), new WeighItems());
    return new WeighingDelegate() {
      @Override
      public void waitFor() {
        myQueue.offer(new Computable.PredefinedValueComputable<>(false));
        try {
          future.get();
        }
        catch (InterruptedException | ExecutionException e) {
          LOG.error(e);
        }
      }

      @Override
      public void consume(final CompletionResult result) {
        if (ourIsInBatchUpdate.get().booleanValue()) {
          myBatchList.add(result);
        }
        else {
          myQueue.offer(() -> {
            tryReadOrCancel(indicator, () -> indicator.addItem(result));
            return true;
          });
        }
      }
    };
  }

  @Override
  protected void flushBatchResult(CompletionProgressIndicator indicator) {
    ArrayList<CompletionResult> batchListCopy = new ArrayList<>(myBatchList);
    myBatchList.clear();

    myQueue.offer(() -> {
      tryReadOrCancel(indicator, () ->
      indicator.withSingleUpdate(() -> {
        for (CompletionResult result : batchListCopy) {
          indicator.addItem(result);
        }
      }));
      return true;
    });
  }

  static void tryReadOrCancel(ProgressIndicator indicator, Runnable runnable) {
    if (!ApplicationManagerEx.getApplicationEx().tryRunReadAction(() -> {
      indicator.checkCanceled();
      runnable.run();
    })) {
      indicator.cancel();
      indicator.checkCanceled();
    }
  }
}

