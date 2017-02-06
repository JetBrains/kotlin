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

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.JCDiagnostic
import com.sun.tools.javac.util.Log
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedWriter
import java.io.PrintWriter
import javax.tools.JavaFileObject

class KaptJavaLog(
        context: Context,
        errWriter: PrintWriter,
        warnWriter: PrintWriter,
        noticeWriter: PrintWriter,
        val interceptorData: DiagnosticInterceptorData
) : Log(context, errWriter, warnWriter, noticeWriter) {
    init {
        context.put(Log.outKey, noticeWriter)
    }

    override fun report(diagnostic: JCDiagnostic) {
        if (diagnostic.type == JCDiagnostic.DiagnosticType.ERROR && diagnostic.code in IGNORED_DIAGNOSTICS) {
            return
        }

        val targetElement = diagnostic.diagnosticPosition
        if (diagnostic.code.contains("err.cant.resolve") && targetElement != null) {
            val sourceFile = interceptorData.files[diagnostic.source]
            if (sourceFile != null) {
                val insideImports = targetElement.tree in sourceFile.imports
                // Ignore resolve errors in import statements
                if (insideImports) return
            }
        }

        super.report(diagnostic)
    }

    private operator fun <T : JCTree> Iterable<T>.contains(element: JCTree?): Boolean {
        if (element == null) {
            return false
        }

        var found = false
        val visitor = object : JCTree.Visitor() {
            override fun visitImport(that: JCTree.JCImport) {
                super.visitImport(that)
                if (!found) that.qualid.accept(this)
            }

            override fun visitSelect(that: JCTree.JCFieldAccess) {
                super.visitSelect(that)
                if (!found) that.selected.accept(this)
            }

            override fun visitTree(that: JCTree) {
                if (!found && element == that) found = true
            }
        }
        this.forEach { if (!found) it.accept(visitor) }
        return found
    }

    companion object {
        private val IGNORED_DIAGNOSTICS = setOf(
                "compiler.err.name.clash.same.erasure",
                "compiler.err.name.clash.same.erasure.no.override",
                "compiler.err.name.clash.same.erasure.no.override.1",
                "compiler.err.name.clash.same.erasure.no.hide",
                "compiler.err.already.defined",
                "compiler.err.annotation.type.not.applicable",
                "compiler.err.doesnt.exist")

        internal fun preRegister(context: Context, messageCollector: MessageCollector) {
            val interceptorData = DiagnosticInterceptorData()
            context.put(Log.logKey, Context.Factory<Log> {
                fun makeWriter(severity: CompilerMessageSeverity) = PrintWriter(MessageCollectorBackedWriter(messageCollector, severity))
                val errWriter = makeWriter(ERROR)
                val warnWriter = makeWriter(STRONG_WARNING)
                val noticeWriter = makeWriter(INFO)
                KaptJavaLog(it, errWriter, warnWriter, noticeWriter, interceptorData)
            })
        }
    }

    class DiagnosticInterceptorData {
        var files: Map<JavaFileObject, JCTree.JCCompilationUnit> = emptyMap()
    }
}