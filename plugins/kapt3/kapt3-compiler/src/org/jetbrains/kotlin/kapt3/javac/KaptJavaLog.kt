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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.*
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.kotlin.kapt3.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedWriter
import org.jetbrains.kotlin.kapt3.util.isJava9OrLater
import java.io.File
import java.io.PrintWriter
import javax.tools.JavaFileObject
import com.sun.tools.javac.util.List as JavacList

class KaptJavaLog(
        private val project: Project,
        context: Context,
        errWriter: PrintWriter,
        warnWriter: PrintWriter,
        noticeWriter: PrintWriter,
        val interceptorData: DiagnosticInterceptorData
) : Log(context, errWriter, warnWriter, noticeWriter) {
    private val stubLineInfo = KaptStubLineInformation()

    private val diagnosticFactory by lazy { JCDiagnostic.Factory.instance(context) }
    private val javacMessages by lazy { JavacMessages.instance(context) }

    init {
        context.put(Log.outKey, noticeWriter)
    }

    val reportedDiagnostics: List<JCDiagnostic>
        get() = _reportedDiagnostics

    private val _reportedDiagnostics = mutableListOf<JCDiagnostic>()

    override fun flush(kind: WriterKind?) {
        super.flush(kind)

        val diagnosticKind = when (kind) {
            WriterKind.ERROR -> JCDiagnostic.DiagnosticType.ERROR
            WriterKind.WARNING -> JCDiagnostic.DiagnosticType.WARNING
            WriterKind.NOTICE -> JCDiagnostic.DiagnosticType.NOTE
            else -> return
        }

        _reportedDiagnostics.removeAll { it.type == diagnosticKind }
    }

    override fun flush() {
        super.flush()
        _reportedDiagnostics.clear()
    }

    override fun report(diagnostic: JCDiagnostic) {
        if (diagnostic.type == JCDiagnostic.DiagnosticType.ERROR && diagnostic.code in IGNORED_DIAGNOSTICS) {
            return
        }

        if (diagnostic.type == JCDiagnostic.DiagnosticType.WARNING
            && diagnostic.code == "compiler.warn.proc.unmatched.processor.options"
            && diagnostic.args.singleOrNull() == "[kapt.kotlin.generated]"
        ) {
            // Do not report the warning about "kapt.kotlin.generated" option being ignored if it's the only ignored option
            return
        }

        val targetElement = diagnostic.diagnosticPosition
        val sourceFile = interceptorData.files[diagnostic.source]

        if (diagnostic.code.contains("err.cant.resolve") && targetElement != null) {
            if (sourceFile != null) {
                val insideImports = targetElement.tree in sourceFile.imports
                // Ignore resolve errors in import statements
                if (insideImports) return
            }
        }

        if (sourceFile != null && targetElement.tree != null) {
            val kotlinPosition = stubLineInfo.getPositionInKotlinFile(sourceFile, targetElement.tree)
            val kotlinFile = kotlinPosition?.let { getKotlinSourceFile(it) }
            if (kotlinPosition != null && kotlinFile != null) {
                val locationMessage = "$KOTLIN_LOCATION_PREFIX${kotlinFile.absolutePath}: (${kotlinPosition.line}, ${kotlinPosition.column})"
                val locationDiagnostic = diagnosticFactory.note(null as DiagnosticSource?, null, "proc.messager", locationMessage)
                val wrappedDiagnostic = JCDiagnostic.MultilineDiagnostic(diagnostic, JavacList.of(locationDiagnostic))

                super.report(wrappedDiagnostic)
                _reportedDiagnostics += wrappedDiagnostic

                // Avoid reporting the diagnostic twice
                return
            }
        }

        _reportedDiagnostics += diagnostic

        super.report(diagnostic)
    }

    override fun writeDiagnostic(diagnostic: JCDiagnostic) {
        if (hasDiagnosticListener()) {
            diagListener.report(diagnostic)
            return
        }

        val writer = when (diagnostic.type) {
            DiagnosticType.FRAGMENT, null -> kotlin.error("Invalid root diagnostic type: ${diagnostic.type}")
            DiagnosticType.NOTE -> super.getWriter(WriterKind.NOTICE)
            DiagnosticType.WARNING -> super.getWriter(WriterKind.WARNING)
            DiagnosticType.ERROR -> super.getWriter(WriterKind.ERROR)
        }

        val formattedMessage = diagnosticFormatter.format(diagnostic, javacMessages.currentLocale)
                .lines()
                .joinToString(LINE_SEPARATOR) { original ->
                    // Kotlin location is put as a sub-diagnostic, so the formatter indents it with four additional spaces (6 in total).
                    // It looks weird, especially in the build log inside IntelliJ, so let's make things a bit better.
                    val trimmed = original.trimStart()
                    // Typically, javac places additional details about the diagnostics indented by two spaces
                    if (trimmed.startsWith(KOTLIN_LOCATION_PREFIX)) "  " + trimmed else original
                }

        writer.print(formattedMessage)
        writer.flush()
    }

    private fun getKotlinSourceFile(pos: KotlinPosition): File? {
        return if (pos.isRelativePath) {
            val basePath = project.basePath
            if (basePath != null) File(basePath, pos.path) else null
        }
        else {
            File(pos.path)
        }
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
        private val LINE_SEPARATOR: String = System.getProperty("line.separator")
        private val KOTLIN_LOCATION_PREFIX = "Kotlin location: "

        private val IGNORED_DIAGNOSTICS = setOf(
                "compiler.err.name.clash.same.erasure",
                "compiler.err.name.clash.same.erasure.no.override",
                "compiler.err.name.clash.same.erasure.no.override.1",
                "compiler.err.name.clash.same.erasure.no.hide",
                "compiler.err.already.defined",
                "compiler.err.annotation.type.not.applicable",
                "compiler.err.doesnt.exist")

        internal fun preRegister(kaptContext: KaptContext<*>, messageCollector: MessageCollector) {
            val interceptorData = DiagnosticInterceptorData()
            kaptContext.context.put(Log.logKey, Context.Factory<Log> { newContext ->
                fun makeWriter(severity: CompilerMessageSeverity) = PrintWriter(MessageCollectorBackedWriter(messageCollector, severity))
                val errWriter = makeWriter(ERROR)
                val warnWriter = makeWriter(STRONG_WARNING)
                val noticeWriter = makeWriter(WARNING)
                KaptJavaLog(kaptContext.project, newContext, errWriter, warnWriter, noticeWriter, interceptorData)
            })
        }
    }

    class DiagnosticInterceptorData {
        var files: Map<JavaFileObject, JCTree.JCCompilationUnit> = emptyMap()
    }
}

fun KaptContext<*>.kaptError(text: String): JCDiagnostic {
    return JCDiagnostic.Factory.instance(context).errorJava9Aware(null, null, "proc.messager", text)
}

fun KaptContext<*>.kaptError(text: String, target: PsiElement): JCDiagnostic {
    //TODO provide source binding
    return JCDiagnostic.Factory.instance(context).errorJava9Aware(null, null, "proc.messager", text)
}

private fun JCDiagnostic.Factory.errorJava9Aware(
        source: DiagnosticSource?,
        pos: JCDiagnostic.DiagnosticPosition?,
        key: String,
        vararg args: String
): JCDiagnostic {
    return if (isJava9OrLater) {
        val errorMethod = this::class.java.getDeclaredMethod(
                "error",
                JCDiagnostic.DiagnosticFlag::class.java,
                DiagnosticSource::class.java,
                JCDiagnostic.DiagnosticPosition::class.java,
                String::class.java,
                Array<Any>::class.java)

        errorMethod.invoke(this, JCDiagnostic.DiagnosticFlag.MANDATORY, source, pos, key, args) as JCDiagnostic
    }
    else {
        this.error(source, pos, key, *args)
    }
}