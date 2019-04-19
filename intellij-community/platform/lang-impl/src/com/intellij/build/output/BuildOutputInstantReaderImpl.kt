// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.util.*

/**
 * @author Vladislav.Soroka
 */
open class BuildOutputInstantReaderImpl(private val buildId: Any,
                                   buildProgressListener: BuildProgressListener,
                                   parsers: List<BuildOutputParser>) : BuildOutputInstantReader, Closeable, Appendable {
  private val job: Job
  private val channel = Channel<String>()
  private val receivedLinesBuffer = LinkedList<String>()
  private var currentIndex = -1

  private val lineProcessor: LineProcessor

  init {
    val thisReader = this
    job = GlobalScope.launch(start = CoroutineStart.LAZY) {
      var lastMessage: BuildEvent? = null
      val messageConsumer = { event: BuildEvent ->
        //do not add duplicates, e.g. sometimes same messages can be added both to stdout and stderr
        if (event != lastMessage) {
          buildProgressListener.onEvent(event)
        }
        lastMessage = event
      }

      while (true) {
        val line = thisReader.readLine() ?: break
        if (line.isBlank()) continue

        for (parser in parsers) {
          if (parser.parse(line, BuildOutputInstantReaderWrapper(thisReader), messageConsumer)) {
            break
          }
        }
      }
    }

    lineProcessor = object : LineProcessor() {
      override fun process(line: String) {
        if (job.isCompleted) {
          LOG.warn("Build output reader closed")
          return
        }
        if (!job.isActive) {
          job.start()
        }
        runBlocking { channel.send(line) }
      }
    }
  }

  override fun getBuildId(): Any {
    return buildId
  }

  override fun append(csq: CharSequence): BuildOutputInstantReaderImpl {
    lineProcessor.append(csq)
    return this
  }

  override fun append(csq: CharSequence, start: Int, end: Int): BuildOutputInstantReaderImpl {
    lineProcessor.append(csq, start, end)
    return this
  }

  override fun append(c: Char): BuildOutputInstantReaderImpl {
    lineProcessor.append(c)
    return this
  }

  override fun close() {
    runBlocking {
      lineProcessor.close()
      channel.close()
      job.cancelAndJoin()
    }
  }

  override fun readLine(): String? {
    if (currentIndex < -1) {
      LOG.error("Wrong buffered output lines index")
      currentIndex = -1
    }

    if (receivedLinesBuffer.size > currentIndex + 1) {
      currentIndex++
      return receivedLinesBuffer[currentIndex]
    }
    val line = runBlocking {
      try {
        channel.receive()
      }
      catch (e: ClosedReceiveChannelException) {
        null
      }
    } ?: return null
    receivedLinesBuffer.addLast(line)
    currentIndex++
    if (receivedLinesBuffer.size > getMaxLinesBufferSize()) {
      receivedLinesBuffer.removeFirst()
      currentIndex--
    }
    return line
  }

  override fun pushBack() = pushBack(1)

  override fun pushBack(numberOfLines: Int) {
    currentIndex -= numberOfLines
  }

  override fun getCurrentLine(): String? {
    return if (currentIndex >= 0 && receivedLinesBuffer.size > currentIndex) receivedLinesBuffer[currentIndex] else null
  }

  private class BuildOutputInstantReaderWrapper(private val reader: BuildOutputInstantReaderImpl) : BuildOutputInstantReader {
    private var linesRead = 0

    override fun getBuildId(): Any = reader.buildId

    override fun readLine(): String? {
      val line = reader.readLine()
      if (line != null) linesRead++
      return line
    }

    override fun pushBack() = pushBack(1)

    override fun pushBack(numberOfLines: Int) {
      if (numberOfLines > linesRead) {
        reader.pushBack(linesRead)
      }
      else {
        reader.pushBack(numberOfLines)
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
