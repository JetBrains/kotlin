/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import java.io.Serializable
import kotlin.script.experimental.jvm.impl.KJvmCompiledModule

class KJvmCompiledModuleInMemory(val compilerOutputFiles: Map<String, ByteArray>) : KJvmCompiledModule,
    Serializable {

    override fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader =
        CompiledScriptClassLoader(baseClassLoader, compilerOutputFiles)

    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }
}