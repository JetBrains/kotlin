/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContext
import java.io.File
import kotlin.script.experimental.api.ScriptDefinition
import kotlin.script.experimental.api.ScriptDependency
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.util.PropertiesCollection

open class JvmScriptDefinition : PropertiesCollection.Builder() {

    companion object : JvmScriptDefinition()
}

val ScriptDefinition.jvm get() = JvmScriptDefinition()


class JvmDependency(val classpath: List<File>) : ScriptDependency {
    constructor(vararg classpathEntries: File) : this(classpathEntries.asList())
}

fun JvmScriptDefinition.dependenciesFromCurrentContext(vararg libraries: String, wholeClasspath: Boolean = false) {
    dependenciesFromClassloader(*libraries, wholeClasspath = wholeClasspath)
}

fun JvmScriptDefinition.dependenciesFromClassloader(
    vararg libraries: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
) {
    ScriptDefinition.dependencies.append(
        JvmDependency(scriptCompilationClasspathFromContext(*libraries, classLoader = classLoader, wholeClasspath = wholeClasspath))
    )
}
