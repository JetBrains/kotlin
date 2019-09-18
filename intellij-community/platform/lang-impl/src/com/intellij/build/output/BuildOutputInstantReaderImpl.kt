// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.util.ConcurrencyUtil.underThreadNameRunnable
import org.jetbrains.annotations.ApiStatus
import java.io.Closeable
import java.io.IOException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * @author Vladislav.Soroka
 */
open class BuildOutputInstantReaderImpl @JvmOverloads constructor(
  private val buildId: Any,
  private val parentEventId: Any,
  buildProgressListener: BuildProgressListener,
  parsers: List<BuildOutputParser>,
  private val pushBackBufferSize: Int = 50,
  channelBufferCapacity: Int = 64
) : BuildOutputInstantReader, Closeable, Appendable {
  private val channel = LinkedBlockingQueue<String>(channelBufferCapacity)
  private val readLinesBuffer = LinkedList<String>()
  private var readLinesBufferPosition = -1
  private val state = AtomicReference<State>(State.NotStarted)
  private val readFinishedFuture = CompletableFuture<Unit>()
  @Suppress("LeakingThis")
  private val readerRunnable = underThreadNameRunnable("Reader thread for BuildOutputInstantReaderImpl@${System.identityHashCode(this)}") {
    var lastMessage: BuildEvent? = null
    val messageConsumer = { event: BuildEvent ->
      //do not add duplicates, e.g. sometimes same messages can be added both to stdout and stderr
      if (event != lastMessage) {
        buildProgressListener.onEvent(buildId, event)
      }
      lastMessage = event
    }

    try {
      while (true) {
        val line = readLine() ?: break
        if (line.isBlank()) continue
        for (parser in parsers) {
          val readerWrapper = BuildOutputInstantReaderWrapper(this)
          if (parser.parse(line, readerWrapper, messageConsumer)) break
          readerWrapper.pushBackReadLines()
        }
      }
      readFinishedFuture.complete(Unit)
    }
    catch (ex: Throwable) {
      readFinishedFuture.completeExceptionally(ex)
    }
  }

  private val appendedLineProcessor = object : LineProcessor() {
    override fun process(line: String) {
      require(state.get() != State.Closed) { "Can't append to closed stream" }
      if (state.compareAndSet(State.NotStarted, State.Running)) {
        ProcessIOExecutorService.INSTANCE.submit(readerRunnable)
      }
      try {
        while (state.get() != State.Closed) {
          if (channel.offer(line, 100, TimeUnit.MILLISECONDS)) {
            break
          }
        }
      }
      catch (e: InterruptedException) {
        throw IOException(e)
      }
    }
  }

  override fun getParentEventId() = parentEventId

  override fun append(csq: CharSequence): BuildOutputInstantReaderImpl {
    appendedLineProcessor.append(csq)
    return this
  }

  override fun append(csq: CharSequence, start: Int, end: Int): BuildOutputInstantReaderImpl {
    appendedLineProcessor.append(csq, start, end)
    return this
  }

  override fun append(c: Char): BuildOutputInstantReaderImpl {
    appendedLineProcessor.append(c)
    return this
  }

  override fun close() {
    closeAndGetFuture()
  }

  open fun closeAndGetFuture(): CompletableFuture<Unit> {
    if (state.get() == State.Closed) return readFinishedFuture
    if (state.compareAndSet(State.NotStarted, State.Closed)) {
      readFinishedFuture.complete(Unit)
    }
    else {
      state.set(State.Closed)
    }
    return readFinishedFuture
  }

  override fun readLine(): String? {
    if (readLinesBufferPosition >= 0) {
      return readLinesBuffer[readLinesBufferPosition].also { readLinesBufferPosition-- }
    }
    var line: String?
    while (true) {
      line = channel.poll(100, TimeUnit.MILLISECONDS)
      if (line != null || state.get() == State.Closed) break
    }
    if (line == null) return line;
    readLinesBuffer.addFirst(line)
    if (readLinesBuffer.size > pushBackBufferSize) {
      readLinesBuffer.removeLast()
    }
    return line
  }

  override fun pushBack() = pushBack(1)

  override fun pushBack(numberOfLines: Int) {
    readLinesBufferPosition += numberOfLines
  }

  private class BuildOutputInstantReaderWrapper(private val reader: BuildOutputInstantReader) : BuildOutputInstantReader {
    private var linesRead = 0

    override fun getParentEventId(): Any = reader.parentEventId

    override fun readLine(): String? {
      val line = reader.readLine()
      if (line != null) linesRead++
      return line
    }

    override fun pushBack() = pushBack(1)

    override fun pushBack(numberOfLines: Int) {
      val numberToPushBack = if (numberOfLines > linesRead) linesRead else numberOfLines
      linesRead -= numberToPushBack
      reader.pushBack(numberToPushBack)
    }

    fun pushBackReadLines() {
      if (linesRead != 0) {
        reader.pushBack(linesRead)
        linesRead = 0
      }
    }
  }

  companion object {
    private enum class State { NotStarted, Running, Closed }
  }
}

@ApiStatus.Experimental
class BuildOutputCollector(private val reader: BuildOutputInstantReader) : BuildOutputInstantReader {
  private val readLines = LinkedList<String>()
  override fun getParentEventId(): Any = reader.parentEventId

  override fun readLine(): String? {
    val line = reader.readLine()
    if (line != null) {
      readLines.add(line)
    }
    return line
  }

  override fun pushBack() {
    reader.pushBack()
    readLines.pollLast()
  }

  override fun pushBack(numberOfLines: Int) {
    reader.pushBack(numberOfLines)
    repeat(numberOfLines) { readLines.pollLast() ?: return@repeat }

  }

  fun getOutput(): String = readLines.joinToString(separator = "\n")
}