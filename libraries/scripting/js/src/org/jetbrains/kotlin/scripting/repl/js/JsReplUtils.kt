/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.reflect.KClass
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration

class JsState(override val lock: ReentrantReadWriteLock) : IReplStageState<ScriptDescriptor> {
    override val history: IReplStageHistory<ScriptDescriptor>
        get() = TODO("not implemented")

    override val currentGeneration: Int
        get() = TODO("not implemented")

}

class CompiledToJsScript(
    val jsCode: String,
    override val compilationConfiguration: ScriptCompilationConfiguration
) : CompiledScript<Any> {
    override suspend fun getClass(scriptEvaluationConfiguration: ScriptEvaluationConfiguration?): ResultWithDiagnostics<KClass<*>> {
        throw IllegalStateException("Class is not available for JS implementation")
    }
}
