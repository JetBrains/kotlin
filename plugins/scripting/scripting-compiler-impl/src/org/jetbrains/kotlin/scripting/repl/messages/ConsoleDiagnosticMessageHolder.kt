/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.messages

import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorBasedReporter
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ConsoleDiagnosticMessageHolder : MessageCollectorBasedReporter, DiagnosticMessageHolder {
    private val outputStream = ByteArrayOutputStream()

    override val messageCollector: GroupingMessageCollector = GroupingMessageCollector(
        PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.WITHOUT_PATHS, false),
        false
    )

    override fun renderMessage(): String {
        messageCollector.flush()
        return outputStream.toString("UTF-8")
    }
}
