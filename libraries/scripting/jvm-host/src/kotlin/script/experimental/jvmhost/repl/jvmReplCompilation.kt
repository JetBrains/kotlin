/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.repl

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.repl.IReplStageHistory
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import kotlin.script.experimental.api.*

interface KJvmReplCompilerProxy {
    fun createReplCompilationState(scriptCompilationConfiguration: ScriptCompilationConfiguration): JvmReplCompilerState.Compilation

    fun checkSyntax(
        script: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        project: Project
    ): ResultWithDiagnostics<Boolean>


    fun compileReplSnippet(
        compilationState: JvmReplCompilerState.Compilation,
        snippet: SourceCode,
        snippetId: ReplSnippetId,
        history: IReplStageHistory<ScriptDescriptor>
    ): ResultWithDiagnostics<CompiledScript<*>>
}