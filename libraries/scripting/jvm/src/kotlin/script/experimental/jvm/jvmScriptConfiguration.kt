/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import kotlinx.coroutines.experimental.runBlocking
import kotlin.script.experimental.api.*
import java.io.File
import kotlin.script.experimental.jvm.JvmScriptCompileConfigurationParams.javaHomeDir

inline fun jvmConfigWithJavaHome(
    from: HeterogeneousMap = HeterogeneousMap(),
    crossinline body: JvmScriptCompileConfigurationParams.Builder.() -> Unit = {}
) =
    jvmScriptConfiguration(from) {
        javaHomeDir(File(System.getProperty("java.home")))
        body()
    }

val ScriptCompilationConfigurator?.defaultConfiguration: ScriptCompileConfiguration
    get() = this?.let { runBlocking { defaultConfiguration } } ?: ScriptCompileConfiguration()