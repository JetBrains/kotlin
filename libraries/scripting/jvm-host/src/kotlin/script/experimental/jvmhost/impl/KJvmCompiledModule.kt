/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.impl

import java.io.File
import java.io.Serializable
import java.net.URLClassLoader

interface KJvmCompiledModule {
    fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader
}

class KJvmCompiledModuleInMemory(val compilerOutputFiles: Map<String, ByteArray>) : KJvmCompiledModule, Serializable {

    override fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader =
        CompiledScriptClassLoader(baseClassLoader, compilerOutputFiles)

    companion object {
        @JvmStatic
        private val serialVersionUID = 0L
    }
}

class KJvmCompiledModuleFromClassPath(val classpath: Collection<File>) : KJvmCompiledModule {

    override fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader =
        URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseClassLoader)
}

class KJvmCompiledModuleFromLoadedClasses : KJvmCompiledModule {

    override fun createClassLoader(baseClassLoader: ClassLoader?): ClassLoader =
        baseClassLoader ?: KJvmCompiledModuleFromLoadedClasses::class.java.classLoader
}
