/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.Writer

class MessageCollectorBackedWriter(
    private val messageCollector: MessageCollector,
    private val severity: CompilerMessageSeverity
) : Writer() {
    override fun write(buffer: CharArray, offset: Int, length: Int) {
        val message = String(buffer, offset, length).trim().trim('\n', '\r')
        if (message.isNotEmpty()) {
            messageCollector.report(severity, message)
        }
    }

    override fun flush() {
        if (messageCollector is GroupingMessageCollector) {
            messageCollector.flush()
        }
    }

    override fun close() {
        flush()
    }
}
