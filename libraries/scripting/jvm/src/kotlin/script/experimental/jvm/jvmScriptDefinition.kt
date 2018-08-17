/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContext
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.util.PropertiesCollection

data class JvmDependency(val classpath: List<File>) : ScriptDependency {
    @Suppress("unused")
    constructor(vararg classpathEntries: File) : this(classpathEntries.asList())
}

interface JvmScriptDefinitionKeys

open class JvmScriptDefinitionBuilder : PropertiesCollection.Builder(), JvmScriptDefinitionKeys {
    companion object : PropertiesCollection.Builder.BuilderExtension<JvmScriptDefinitionBuilder>, JvmScriptDefinitionKeys {
        override fun get() = JvmScriptDefinitionBuilder()
    }
}

fun JvmScriptDefinitionBuilder.dependenciesFromCurrentContext(vararg libraries: String, wholeClasspath: Boolean = false) {
    dependenciesFromClassloader(*libraries, wholeClasspath = wholeClasspath)
}

fun JvmScriptDefinitionBuilder.dependenciesFromClassloader(
    vararg libraries: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
) {
    ScriptDefinition.dependencies.append(
        JvmDependency(scriptCompilationClasspathFromContext(*libraries, classLoader = classLoader, wholeClasspath = wholeClasspath))
    )
}

val JvmScriptDefinitionKeys.javaHome by PropertiesCollection.keyCopy(ScriptingEnvironment.jvm.javaHome)

@Suppress("unused")
val ScriptDefinitionKeys.jvm
    get() = JvmScriptDefinitionBuilder

