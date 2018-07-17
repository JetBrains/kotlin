/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.api.ScriptDefinitionProperties
import kotlin.script.experimental.api.ScriptingProperties
import kotlin.script.experimental.api.addToListProperty

fun ScriptingProperties.jvmDependenciesFromCurrentContext(vararg libraries: String, wholeClasspath: Boolean = false) {
    jvmDependenciesFromClassloader(*libraries, wholeClasspath = wholeClasspath)
}

fun ScriptingProperties.jvmDependenciesFromClassloader(
    vararg libraries: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
) {
    data.addToListProperty(
        ScriptDefinitionProperties.dependencies,
        JvmDependency(scriptCompilationClasspathFromContext(*libraries, classLoader = classLoader, wholeClasspath = wholeClasspath))
    )
}
