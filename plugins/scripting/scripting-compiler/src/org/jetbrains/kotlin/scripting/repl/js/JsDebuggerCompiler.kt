/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import java.util.concurrent.locks.ReentrantReadWriteLock

class JsDebuggerCompiler(
    environment: KotlinCoreEnvironment,
    loadNames: NameTables
) : ReplCompiler {
    // TODO: configure dependencies
    private val compiler = CoreScriptingJsCompiler(
        environment,
        loadNames,
        readLibrariesFromConfiguration(environment.configuration)
    )

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> {
        return JsState(lock)
    }

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult {
        return ReplCheckResult.Ok()
    }

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult {
        return compiler.compile(codeLine)
    }
}
