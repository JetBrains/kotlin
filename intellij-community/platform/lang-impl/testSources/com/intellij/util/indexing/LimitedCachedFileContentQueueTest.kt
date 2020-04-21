// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.testFramework.SkipSlowTestLocally
import com.intellij.testFramework.fixtures.BareTestFixtureTestCase
import com.intellij.util.indexing.caches.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@SkipSlowTestLocally
class LimitedCachedFileContentQueueTest : BareTestFixtureTestCase() {

  @Test
  fun `load single file`() {
    val virtualFile = BinaryLightVirtualFile("a", createByteArray(5))
    val limiter = MaxTotalSizeCachedFileLoadLimiter(10)
    val contentLoader = CachedFileContentLoader { file ->
      CachedFileContent(file)
    }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(virtualFile), limiter, contentLoader)
    val indicator = createEmptyIndicator()
    val thread = createThread {
      val token = queue.loadNextContent(indicator)!!
      assertArrayEquals(virtualFile.contentsToByteArray(), token.content.bytes)
      token.release()
    }
    thread.waitForStart()
    thread.waitForFinish()
  }

  @Test
  fun `thread is blocked on pulling the next content if there is no room left in the queue`() {
    val oneFile = BinaryLightVirtualFile("a", createByteArray(100))
    val twoFile = BinaryLightVirtualFile("b", createByteArray(1))

    val limiter = MaxTotalSizeCachedFileLoadLimiter(100)
    val contentLoader = CachedFileContentLoader { file ->
      CachedFileContent(file)
    }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(oneFile, twoFile), limiter, contentLoader)

    val onReleaseToken = CountDownLatch(1)
    val firstThread = createThread {
      //Loads the content immediately.
      val oneToken = queue.loadNextContent(createEmptyIndicator())!!
      onReleaseToken.await()
      oneToken.release()
    }
    firstThread.waitForStart()
    firstThread.waitForBlock()

    val secondThread = createThread {
      //Should block because there is no room left in the queue.
      val token = queue.loadNextContent(createEmptyIndicator())!!
      token.release()
    }
    secondThread.waitForStart()
    secondThread.waitForBlock()

    onReleaseToken.countDown()

    firstThread.waitForFinish()
    secondThread.waitForFinish()
  }

  @Test
  fun `pushed back content must be taken by another thread`() {
    val file = BinaryLightVirtualFile("a", createByteArray(100))

    val limiter = MaxTotalSizeCachedFileLoadLimiter(100)
    val contentLoader = CachedFileContentLoader {
      CachedFileContent(it)
    }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(file), limiter, contentLoader)

    val onPushBack = CountDownLatch(1)
    val firstThreadToken = AtomicReference<CachedFileContentToken>()
    val firstThread = createThread {
      val token = queue.loadNextContent(createEmptyIndicator())!!
      firstThreadToken.set(token)
      onPushBack.await()
      //Push back the content. It must be taken by the other thread then.
      token.pushBack()
    }

    val secondThreadToken = AtomicReference<CachedFileContentToken>()
    val secondThread = createThread {
      //Blocks until the content is pushed back by the first thread.
      val otherToken = queue.loadNextContent(createEmptyIndicator())
      secondThreadToken.set(otherToken)
    }

    firstThread.waitForStart()
    firstThread.waitForBlock()
    assertEquals(file, firstThreadToken.get().content.virtualFile)

    secondThread.waitForStart()
    secondThread.waitForBlock()

    onPushBack.countDown()
    firstThread.waitForFinish()
    secondThread.waitForFinish()

    val content = secondThreadToken.get().content
    assertEquals(file, content.virtualFile)
  }

  @Test
  fun `file whose loading has been cancelled will be processed later`() {
    val aFile = BinaryLightVirtualFile("a", createByteArray(1))

    val limiter = MaxTotalSizeCachedFileLoadLimiter(100)
    val timeToAct = AtomicBoolean()
    val shouldCancel = AtomicBoolean()
    val contentLoader = CachedFileContentLoader { file ->
      @Suppress("ControlFlowWithEmptyBody")
      while (!timeToAct.get()) {
      }
      if (shouldCancel.get()) {
        throw ProcessCanceledException()
      }
      CachedFileContent(file)
    }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(aFile), limiter, contentLoader)

    // Cancel on the first access.
    timeToAct.set(false)
    shouldCancel.set(true)

    val wasCancelled = AtomicBoolean()
    val firstThread = createThread {
      try {
        queue.loadNextContent(createEmptyIndicator())
      }
      catch (e: ProcessCanceledException) {
        wasCancelled.set(true)
      }
    }
    firstThread.waitForStart()
    timeToAct.set(true)
    firstThread.waitForFinish()
    assertTrue(wasCancelled.get())

    // Load on the second access.
    timeToAct.set(false)
    shouldCancel.set(false)

    val wasLoaded = AtomicBoolean()
    val secondThread = createThread {
      val token = queue.loadNextContent(createEmptyIndicator())!!
      wasLoaded.set(true)
      token.release()
    }

    secondThread.waitForStart()
    timeToAct.set(true)
    secondThread.waitForFinish()
    assertTrue(wasLoaded.get())
  }

  @Test
  fun `consider file has been processed if its content was failed to load`() {
    val aFile = BinaryLightVirtualFile("a", createByteArray(1))

    val limiter = MaxTotalSizeCachedFileLoadLimiter(100)
    val contentLoader = CachedFileContentLoader { file ->
      throw FailedToLoadContentException(file, RuntimeException())
    }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(aFile), limiter, contentLoader)

    val failedException = AtomicReference<FailedToLoadContentException>()
    val firstThread = createThread {
      try {
        queue.loadNextContent(createEmptyIndicator())
      }
      catch (e: FailedToLoadContentException) {
        failedException.set(e)
      }
    }
    firstThread.waitForStart()
    firstThread.waitForFinish()
    assertEquals(aFile, failedException.get().file)

    val queueIsEmpty = AtomicBoolean()
    val secondThread = createThread {
      queueIsEmpty.set(queue.loadNextContent(createEmptyIndicator()) == null)
    }
    secondThread.waitForStart()
    secondThread.waitForFinish()

    assertTrue(queueIsEmpty.get())
  }

  @Test
  fun `content queue throws TooLargeContentException if too large file is loaded`() {
    val limitBytes = 100L
    val largeFile = BinaryLightVirtualFile("a", createByteArray((limitBytes + 1).toInt()))
    val limiter = MaxTotalSizeCachedFileLoadLimiter(limitBytes)
    val contentLoader = CachedFileContentLoader { error("Mustn't be called") }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(largeFile), limiter, contentLoader)

    val exception = AtomicReference<TooLargeContentException>()
    val thread = createThread {
      try {
        queue.loadNextContent(createEmptyIndicator())
      }
      catch (e: TooLargeContentException) {
        exception.set(e)
      }
    }
    thread.waitForStart()
    thread.waitForFinish()

    assertEquals(largeFile, exception.get().file)

    val checkEmptyThread = createThread {
      assertNull(queue.loadNextContent(createEmptyIndicator()))
    }
    checkEmptyThread.waitForStart()
    checkEmptyThread.waitForFinish()
  }

  @Test
  fun `single file limiter must block the second thread`() {
    val aFile = BinaryLightVirtualFile("a", createByteArray(1))
    val bFile = BinaryLightVirtualFile("b", createByteArray(1))

    val limiter = UnlimitedSingleCachedFileLoadLimiter()

    val contentLoader = CachedFileContentLoader { CachedFileContent(it) }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(aFile, bFile), limiter, contentLoader)

    val onUnlockFile = CountDownLatch(1)
    val firstThread = createThread {
      val token = queue.loadNextContent(createEmptyIndicator())!!
      onUnlockFile.await()
      token.release()
    }
    firstThread.waitForStart()

    val secondThread = createThread {
      queue.loadNextContent(createEmptyIndicator())
    }
    secondThread.waitForStart()
    secondThread.waitForBlock()

    onUnlockFile.countDown()
    firstThread.waitForFinish()
    secondThread.waitForFinish()
  }

  @Test
  fun `queue must not return null until the last content has been loaded`() {
    val singleFile = BinaryLightVirtualFile("a", createByteArray(1))

    val limiter = MaxTotalSizeCachedFileLoadLimiter(10)

    val finishLoadingContent = CountDownLatch(1)
    val contentLoader = CachedFileContentLoader {
      finishLoadingContent.await()
      CachedFileContent(it)
    }

    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(listOf(singleFile), limiter, contentLoader)
    val loadingThread = createThread {
      val token = queue.loadNextContent(createEmptyIndicator())!!
      token.release()
    }

    val blockedThreadGotNull = AtomicBoolean()
    val blockedThread = createThread {
      blockedThreadGotNull.set(queue.loadNextContent(createEmptyIndicator()) == null)
    }

    loadingThread.waitForStart()
    loadingThread.waitForBlock() // Blocks on loading the only content.

    blockedThread.waitForStart()
    blockedThread.waitForBlock() // Blocks because the 1st thread is loading the content.

    finishLoadingContent.countDown()

    loadingThread.waitForFinish()
    blockedThread.waitForFinish()

    assertTrue(blockedThreadGotNull.get())
  }

  @Test
  fun `add file to the queue after the queue has been emptied`() {
    val limiter = MaxTotalSizeCachedFileLoadLimiter(10)

    val contentLoader = CachedFileContentLoader { CachedFileContent(it) }

    val queue = LimitedCachedFileContentQueue.createEmptyAppendable(limiter, contentLoader)

    val firstFile = BinaryLightVirtualFile("a", createByteArray(1))
    queue.addFileToLoad(firstFile)

    val loadFirstFileThread = createThread {
      val token = queue.loadNextContent(createEmptyIndicator())!!
      assertEquals(firstFile, token.content.virtualFile)
      token.release()
    }

    loadFirstFileThread.waitForStart()
    loadFirstFileThread.waitForFinish()

    val checkEmptyThread = createThread {
      assertNull(queue.loadNextContent(createEmptyIndicator()))
    }
    checkEmptyThread.waitForStart()
    checkEmptyThread.waitForFinish()

    val secondFile = BinaryLightVirtualFile("b", createByteArray(1))
    queue.addFileToLoad(secondFile)

    val loadSecondFileThread = createThread {
      val token = queue.loadNextContent(createEmptyIndicator())!!
      assertEquals(secondFile, token.content.virtualFile)
      token.release()
    }
    loadSecondFileThread.waitForStart()
    loadSecondFileThread.waitForFinish()
  }

  @Test
  fun `stress test that queue does not hold reserved bytes when it gets emptied`() {
    val threadsN = 8
    val executor = Executors.newFixedThreadPool(threadsN)

    val filesN = 100
    val fileSize = 1
    val maxFilesInMemory = 3L
    val files = (0 until filesN).map { BinaryLightVirtualFile("a-$it", createByteArray(fileSize)) }

    repeat(100) {
      val limiter = MaxTotalSizeCachedFileLoadLimiter(maxFilesInMemory)
      val contentLoader = CachedFileContentLoader { CachedFileContent(it) }

      val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(files, limiter, contentLoader)

      val anyException = AtomicReference<Throwable>()
      val threadsFinished = CountDownLatch(threadsN)
      repeat(threadsN) {
        executor.submit {
          try {
            while (true) {
              if (anyException.get() != null) break
              val token = queue.loadNextContent(createEmptyIndicator())
              if (token == null) {
                assertEquals(0, limiter.loadedBytes)
                break
              }
              token.release()
            }
          } catch (e: Throwable) {
            anyException.set(e)
          } finally {
            threadsFinished.countDown()
          }
        }
      }

      if (!threadsFinished.await(60, TimeUnit.SECONDS)) {
        fail("The test is too slow")
      }
      anyException.get()?.let { throw it }
    }
    executor.shutdownNow()
    executor.awaitTermination(1, TimeUnit.MINUTES)
  }

  @Test
  fun `stress test queue does not load too much data`() {
    val threadsN = 8
    val filesN = 1000
    val fileSize = 10
    val maxLoadedLimit = 30L //Max 3 files can be loaded in memory.
    val limiter = MaxTotalSizeCachedFileLoadLimiter(maxLoadedLimit)

    val filesProcessed = AtomicInteger()
    val loadedBytes = AtomicLong()
    val tooMuchLoaded = AtomicBoolean()
    fun checkLimit() {
      if (loadedBytes.get() > maxLoadedLimit) {
        tooMuchLoaded.set(true)
        fail("Too much data has been loaded")
      }
    }

    val files = (0 until filesN).map { BinaryLightVirtualFile("a-$it", createByteArray(fileSize)) }
    val contentLoader = CachedFileContentLoader { file ->
      if (ThreadLocalRandom.current().nextInt(100) < 20) {
        //20% chance of FailedToLoadContentException
        throw FailedToLoadContentException(file, RuntimeException())
      }
      val content = CachedFileContent(file)
      content.bytes
      loadedBytes.addAndGet(content.length)
      content
    }
    val queue = LimitedCachedFileContentQueue.createNonAppendableForFiles(files, limiter, contentLoader)

    val executor = Executors.newFixedThreadPool(threadsN)
    val indicator = createEmptyIndicator()
    val threadsFinished = AtomicInteger()
    repeat(threadsN) {
      executor.submit {
        while (true) {
          checkLimit()
          val token = try {
            queue.loadNextContent(indicator)
          }
          catch (e: FailedToLoadContentException) {
            filesProcessed.incrementAndGet()
            continue
          }

          if (token == null) break //No more contents

          //Emulate indexing.
          Thread.sleep(10)

          if (ThreadLocalRandom.current().nextBoolean()) {
            loadedBytes.addAndGet(-token.content.length)
            filesProcessed.incrementAndGet()
            token.release()
          }
          else {
            token.pushBack()
          }
        }
        threadsFinished.incrementAndGet()
      }
    }

    val startTime = System.currentTimeMillis()
    while (threadsFinished.get() != threadsN) {
      checkLimit()
      Thread.sleep(5)
      if (System.currentTimeMillis() - startTime > 1000 * 60) {
        fail("The test is too slow")
      }
    }
    executor.shutdownNow()
    executor.awaitTermination(1, TimeUnit.MINUTES)

    //Check that there are no remaining files in the queue.
    val queueIsEmpty = AtomicBoolean()
    val thread = createThread {
      queueIsEmpty.set(queue.loadNextContent(indicator) == null)
    }
    thread.waitForStart()
    thread.waitForFinish()
    assertTrue(queueIsEmpty.get())

    assertEquals(filesN, filesProcessed.get())
    assertFalse("At some point the queue has loaded too much data", tooMuchLoaded.get())
  }

  private fun createByteArray(size: Int) = ByteArray(size) { it.toByte() }

  private fun createEmptyIndicator() = EmptyProgressIndicator(ModalityState.NON_MODAL)
}