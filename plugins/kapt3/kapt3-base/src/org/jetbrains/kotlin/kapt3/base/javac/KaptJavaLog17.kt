/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.javac

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.*
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType
import org.jetbrains.kotlin.kapt3.base.stubs.KaptStubLineInformation
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import java.io.*
import javax.tools.Diagnostic

// It's copy/paste as is of KaptJavaLog
// TODO: most likely KaptJavaLog could be substituted with this version.
// The only difference that is writers are passed through context
class KaptJavaLog17(
    private val projectBaseDir: File?,
    context: Context,
    override val interceptorData: KaptJavaLogBase.DiagnosticInterceptorData,
    private val mapDiagnosticLocations: Boolean
) : Log(context), KaptJavaLogBase {
    private val stubLineInfo = KaptStubLineInformation()
    private val javacMessages = JavacMessages.instance(context)

    override val reportedDiagnostics: List<JCDiagnostic>
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

        if (mapDiagnosticLocations && sourceFile != null && targetElement?.tree != null) {
            val kotlinPosition = stubLineInfo.getPositionInKotlinFile(sourceFile, targetElement.tree)
            val kotlinFile = kotlinPosition?.let { getKotlinSourceFile(it) }
            if (kotlinPosition != null && kotlinFile != null) {
                val flags = JCDiagnostic.DiagnosticFlag.values().filterTo(mutableSetOf(), diagnostic::isFlagSet)

                val kotlinDiagnostic = diags.create(
                    diagnostic.type,
                    diagnostic.lintCategory,
                    flags,
                    DiagnosticSource(KotlinFileObject(kotlinFile), this),
                    JCDiagnostic.SimpleDiagnosticPosition(kotlinPosition.pos),
                    diagnostic.code.stripCompilerKeyPrefix(),
                    *diagnostic.args
                )

                reportDiagnostic(kotlinDiagnostic)

                // Avoid reporting the diagnostic twice
                return
            }
        }

        reportDiagnostic(diagnostic)
    }

    private fun String.stripCompilerKeyPrefix(): String {
        for (kind in listOf("err", "warn", "misc", "note")) {
            val prefix = "compiler.$kind."
            if (startsWith(prefix)) {
                return drop(prefix.length)
            }
        }

        return this
    }

    private fun reportDiagnostic(diagnostic: JCDiagnostic) {
        if (diagnostic.kind == Diagnostic.Kind.ERROR) {
            val oldErrors = nerrors
            super.report(diagnostic)
            if (nerrors > oldErrors) {
                _reportedDiagnostics += diagnostic
            }
        } else if (diagnostic.kind == Diagnostic.Kind.WARNING) {
            val oldWarnings = nwarnings
            super.report(diagnostic)
            if (nwarnings > oldWarnings) {
                _reportedDiagnostics += diagnostic
            }
        } else {
            super.report(diagnostic)
        }
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
            .joinToString(LINE_SEPARATOR, postfix = LINE_SEPARATOR) { original ->
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
            val basePath = this.projectBaseDir
            if (basePath != null) File(basePath, pos.path) else null
        } else {
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
            "compiler.err.doesnt.exist",
            "compiler.err.cant.resolve.location",
            "compiler.err.duplicate.annotation.missing.container",
            "compiler.err.not.def.access.package.cant.access",
            "compiler.err.package.not.visible",
            "compiler.err.not.def.public.cant.access"
        )
    }
}