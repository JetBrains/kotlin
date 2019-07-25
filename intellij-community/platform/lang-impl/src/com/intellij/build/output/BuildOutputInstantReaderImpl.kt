// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.future.future
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Vladislav.Soroka
 */
open class BuildOutputInstantReaderImpl(private val buildId: Any,
                                        private val parentEventId: Any,
                                        buildProgressListener: BuildProgressListener,
                                        parsers: List<BuildOutputParser>) : BuildOutputInstantReader, Closeable, Appendable {
  private val readJob: Job
  private val appendParentJob: Job = Job()
  private val outputLinesChannel = Channel<String>()

  private val readLinesBuffer = LinkedList<String>()
  private var readLinesBufferPosition = -1
  private val appendedLineProcessor: LineProcessor

  init {
    readJob = createReadJob(buildProgressListener, parsers)
    val appendScope = CoroutineScope(Dispatchers.Default + appendParentJob)
    appendedLineProcessor = MyLineProcessor(readJob, appendScope, outputLinesChannel)
  }

  private fun createReadJob(buildProgressListener: BuildProgressListener,
                            parsers: List<BuildOutputParser>): Job {
    val thisReader: BuildOutputInstantReader = this
    return CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
      var lastMessage: BuildEvent? = null
      val messageConsumer = { event: BuildEvent ->
        //do not add duplicates, e.g. sometimes same messages can be added both to stdout and stderr
        if (event != lastMessage) {
          buildProgressListener.onEvent(buildId, event)
        }
        lastMessage = event
      }

      while (true) {
        val line = thisReader.readLine() ?: break
        if (line.isBlank()) continue

        for (parser in parsers) {
          val readerWrapper = BuildOutputInstantReaderWrapper(thisReader)
          if (parser.parse(line, readerWrapper, messageConsumer)) break
          readerWrapper.pushBackReadLines()
        }
      }
    }
  }

  override fun getParentEventId(): Any {
    return parentEventId
  }

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

  fun closeAndGetFuture(): CompletableFuture<Unit> {
    appendedLineProcessor.close()
    outputLinesChannel.close()
    return CoroutineScope(Dispatchers.Default).future {
      appendParentJob.children.forEach { it.join() }
      appendParentJob.cancelAndJoin()
      readJob.cancelAndJoin()
    }
  }

  override fun readLine(): String? {
    if (readLinesBufferPosition < -1) {
      LOG.error("Wrong buffered output lines index")
      readLinesBufferPosition = -1
    }

    if (readLinesBuffer.size > readLinesBufferPosition + 1) {
      readLinesBufferPosition++
      return readLinesBuffer[readLinesBufferPosition]
    }
    val line = runBlocking {
      try {
        outputLinesChannel.receive()
      }
      catch (e: ClosedReceiveChannelException) {
        null
      }
    } ?: return null
    readLinesBuffer.addLast(line)
    readLinesBufferPosition++
    if (readLinesBuffer.size > getMaxLinesBufferSize()) {
      readLinesBuffer.removeFirst()
      readLinesBufferPosition--
    }
    return line
  }

  override fun pushBack() = pushBack(1)

  override fun pushBack(numberOfLines: Int) {
    readLinesBufferPosition -= numberOfLines
  }

  override fun getCurrentLine(): String? {
    return if (readLinesBufferPosition >= 0 && readLinesBuffer.size > readLinesBufferPosition) readLinesBuffer[readLinesBufferPosition] else null
  }

  private class MyLineProcessor(private val job: Job,
                                private val scope: CoroutineScope,
                                private val channel: Channel<String>) : LineProcessor() {
    @ExperimentalCoroutinesApi
    override fun process(line: String) {
      if (job.isCompleted) {
        LOG.warn("Build output reader closed")
        return
      }
      if (!job.isActive) {
        job.start()
      }
      scope.launch(start = CoroutineStart.UNDISPATCHED) { channel.send(line) }
    }
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

    override fun getCurrentLine(): String? = reader.currentLine
  }

  companion object {
    private val LOG = Logger.getInstance("#com.intellij.build.output.BuildOutputInstantReader")
    @ApiStatus.Experimental
    @TestOnly
    fun getMaxLinesBufferSize() = 50
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

  override fun getCurrentLine(): String = reader.currentLine

  fun getOutput(): String = readLines.joinToString(separator = "\n")
}