/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDependency
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.JvmDependencyFromClassLoader
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

internal fun ScriptCompilationConfiguration.baseCompilationClasspath(): List<File> =
    this[ScriptCompilationConfiguration.dependencies].orEmpty().compilationClasspathOf(this)

// TODO: consider caching (KT-90646)
internal fun Collection<ScriptDependency>.compilationClasspathOf(
    scriptCompilationConfiguration: ScriptCompilationConfiguration,
): List<File> = flatMap {
    when (it) {
        is JvmDependency -> it.classpath
        // FIR has no class-loader based symbol provider yet, so the loader is expanded to its classpath, KT-60443.
        is JvmDependencyFromClassLoader -> scriptCompilationClasspathFromContext(
            classLoader = it.getClassLoader(scriptCompilationConfiguration),
            wholeClasspath = true,
            unpackJarCollections = true,
        )
        else -> emptyList()
    }
}
