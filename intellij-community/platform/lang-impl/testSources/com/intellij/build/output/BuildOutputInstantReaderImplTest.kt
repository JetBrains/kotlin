// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.MessageEvent.Kind.*
import com.intellij.build.events.impl.MessageEventImpl
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.collections.ArrayList

class BuildOutputInstantReaderImplTest {

  @Test
  fun `sanity test`() {
    doTest(mutableListOf(
      createParser("[info]", INFO),
      createParser("[error]", ERROR),
      createParser("[warning]", WARNING)))
  }

  @Test
  fun `test reading with greedy parser`() {
    val greedyParser = BuildOutputParser { _, reader, _ ->
      var count = 0
      while (count < pushBackBufferSize && reader.readLine() != null) {
        count++
      }
      reader.pushBack(count)
      return@BuildOutputParser false
    }

    doTest(mutableListOf(
      createParser("[info]", INFO),
      greedyParser,
      createParser("[error]", ERROR),
      createParser("[warning]", WARNING)))
  }

  @Test
  fun `test bad parser pushed back too many lines`() {
    val badParser = BuildOutputParser { _, reader, _ ->
      reader.pushBack(pushBackBufferSize * 2)
      return@BuildOutputParser false
    }

    doTest(mutableListOf(
      createParser("[info]", INFO),
      badParser,
      createParser("[error]", ERROR),
      createParser("[warning]", WARNING)))
  }

  @Test
  fun `test producer will wait for consumer`() {
    fun line(i: Int) = "line #$i"
    fun String?.appended() = "'$this' appended"
    fun String?.parsed() = "'$this' parsed"

    val eventLog = Collections.synchronizedList(ArrayList<String>())
    val slowParser = BuildOutputParser { line, _, _ ->
      Thread.sleep(100)
      eventLog += line.parsed()
      false
    }
    val lines = (0..5).map(::line)
    val outputReader = BuildOutputInstantReaderImpl(Object(), Object(), BuildProgressListener { _, _ -> }, listOf(slowParser), 0, 1)
    lines.forEach {
      outputReader.appendln(it)
      eventLog += it.appended()
    }
    outputReader.closeAndGetFuture().get()
    val expected = listOf(
      line(0).appended(), line(1).appended(), /* parser keeps each line as additional single line buffer */
      line(0).parsed(),
      line(2).appended(),
      line(1).parsed(),
      line(3).appended(),
      line(2).parsed(),
      line(4).appended(),
      line(3).parsed(),
      line(5).appended(),
      line(4).parsed(), line(5).parsed()
    )
    Assert.assertEquals(expected, eventLog)
  }

  companion object {
    private fun doTest(parsers: MutableList<BuildOutputParser>) {
      val messages = mutableListOf<String>()

      val unparsedLines = mutableListOf<String>()
      val parser = BuildOutputParser { line, _, _ ->
        unparsedLines.add(line)
        true
      }
      parsers.add(parser)
      val buildId = Object()
      val outputReader = BuildOutputInstantReaderImpl(buildId, buildId,
                                                      BuildProgressListener { _, event -> messages += event.message },
                                                      parsers, pushBackBufferSize)
      val trashOut = (0 until pushBackBufferSize).map { "trash" }
      val infoLines = """
        [info] info1
        info2
        info3
      """.trimIndent()
      val warnLines = """
        [warning] warn1
        warn2
      """.trimIndent()
      val errLines = """
        [error] error1
      """.trimIndent()
      val inputData = buildString {
        trashOut.joinTo(this, "\n", postfix = "\n")
        appendln(errLines)
        appendln()
        appendln(infoLines)
        appendln()
        appendln(infoLines) /* checks that duplicate messages are not sent */
        appendln()
        trashOut.joinTo(this, "\n", postfix = "\n")
        appendln(warnLines)
      }
      outputReader.append(inputData).closeAndGetFuture().get()

      Assert.assertEquals(trashOut + trashOut, unparsedLines)
      Assert.assertEquals("""
        error1
        info1
        info2
        info3
        warn1
        warn2
      """.trimIndent(), messages.joinToString("\n"))
    }

    private const val pushBackBufferSize = 50

    private fun createParser(prefix: String, kind: MessageEvent.Kind): BuildOutputParser {
      return BuildOutputParser { line, reader, messageConsumer ->
        if (line.startsWith(prefix)) {
          val buf = StringBuilder()
          var nextLine: String? = line.dropWhile { !it.isWhitespace() }.trimStart()
          while (!nextLine.isNullOrBlank() && !nextLine.startsWith('[')) {
            buf.appendln(nextLine)
            nextLine = reader.readLine()
          }
          messageConsumer.accept(MessageEventImpl(reader.parentEventId, kind, null, buf.toString().dropLast(1), null))
          return@BuildOutputParser true
        }
        return@BuildOutputParser false
      }
    }
  }
}
