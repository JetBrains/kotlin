// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.output

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.MessageEvent.Kind.*
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.build.output.BuildOutputInstantReaderImpl.Companion.getMaxLinesBufferSize
import com.intellij.openapi.util.text.StringUtil
import org.junit.Assert
import org.junit.Test

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
      while (count < getMaxLinesBufferSize() && reader.readLine() != null) {
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
      reader.pushBack(getMaxLinesBufferSize() * 2)
      return@BuildOutputParser false
    }

    doTest(mutableListOf(
      createParser("[info]", INFO),
      badParser,
      createParser("[error]", ERROR),
      createParser("[warning]", WARNING)))
  }

  private fun doTest(parsers: MutableList<BuildOutputParser>) {
    val messages = mutableListOf<String>()

    val unparsedLines = mutableListOf<String>()
    val parser = BuildOutputParser { line, _, _ ->
      unparsedLines.add(line)
      return@BuildOutputParser false
    }
    parsers.add(parser)
    val buildId = Object()
    val outputReader = BuildOutputInstantReaderImpl(buildId, buildId,
                                                    BuildProgressListener { _, event -> messages += event.message },
                                                    parsers)
    val trashOut = StringUtil.repeat("trash\n", getMaxLinesBufferSize()).trimEnd()
    outputReader
      .append("""
${trashOut.prependIndent("        ")}
        [error] error1

        [info] info1
        info2
        info3
${""/* checks that duplicate messages are not sent */}
        [info] info1
        info2
        info3

${trashOut.prependIndent("        ")}
        [warning] warn1
        warn2

        """.trimIndent()
      )
      .closeAndGetFuture().get()

    Assert.assertEquals(trashOut + '\n' + trashOut, unparsedLines.joinToString(separator = "\n"))
    Assert.assertEquals("""
        error1
        info1
        info2
        info3
        warn1
        warn2
      """.trimIndent(), messages.joinToString(separator = "\n"))
  }

  private fun createParser(prefix: String, kind: MessageEvent.Kind): BuildOutputParser {
    return BuildOutputParser { line, reader, messageConsumer ->
      if (line.startsWith(prefix)) {
        val buf = StringBuilder()
        var nextLine: String? = line.dropWhile { !it.isWhitespace() }.trimStart()
        while (!nextLine.isNullOrBlank() && !nextLine.startsWith('[')) {
          buf.append(nextLine).append('\n')
          nextLine = reader.readLine()
        }
        messageConsumer.accept(MessageEventImpl(reader.parentEventId, kind, null, buf.toString().dropLast(1), null))
        return@BuildOutputParser true
      }
      return@BuildOutputParser false
    }
  }
}
