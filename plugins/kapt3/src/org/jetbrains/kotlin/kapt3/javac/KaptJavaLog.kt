/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3.javac

import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedWriter
import java.io.PrintWriter

class KaptJavaLog(
        context: Context?,
        errWriter: PrintWriter,
        warnWriter: PrintWriter,
        noticeWriter: PrintWriter
) : Log(context, errWriter, warnWriter, noticeWriter) {
    override fun report(diagnostic: JCDiagnostic) {
        if (diagnostic.type == JCDiagnostic.DiagnosticType.ERROR && diagnostic.code in IGNORED_DIAGNOSTICS) {
            return
        }

        super.report(diagnostic)
    }

    companion object {
        private val IGNORED_DIAGNOSTICS = setOf(
                "compiler.err.name.clash.same.erasure",
                "compiler.err.name.clash.same.erasure.no.override",
                "compiler.err.name.clash.same.erasure.no.override.1",
                "compiler.err.name.clash.same.erasure.no.hide",
                "compiler.err.already.defined")

        internal fun preRegister(context: Context, messageCollector: MessageCollector) {
            context.put(Log.logKey, Context.Factory<Log> {
                fun makeWriter(severity: CompilerMessageSeverity) = PrintWriter(MessageCollectorBackedWriter(messageCollector, severity))
                val errWriter = makeWriter(ERROR)
                val warnWriter = makeWriter(STRONG_WARNING)
                val noticeWriter = makeWriter(INFO)
                KaptJavaLog(it, errWriter, warnWriter, noticeWriter)
            })
        }
    }
}