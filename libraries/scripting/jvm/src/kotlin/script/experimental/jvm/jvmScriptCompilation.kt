/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext
import kotlin.script.experimental.util.PropertiesCollection

data class JvmDependency(val classpath: List<File>) : ScriptDependency {
    @Suppress("unused")
    constructor(vararg classpathEntries: File) : this(classpathEntries.asList())
}

interface JvmScriptCompilationConfigurationKeys

open class JvmScriptCompilationConfigurationBuilder : PropertiesCollection.Builder(), JvmScriptCompilationConfigurationKeys {
    companion object : JvmScriptCompilationConfigurationKeys
}

fun JvmScriptCompilationConfigurationBuilder.dependenciesFromClassContext(
    contextClass: KClass<*>, vararg libraries: String, wholeClasspath: Boolean = false
) {
    dependenciesFromClassloader(*libraries, classLoader = contextClass.java.classLoader, wholeClasspath = wholeClasspath)
}

fun JvmScriptCompilationConfigurationBuilder.dependenciesFromCurrentContext(vararg libraries: String, wholeClasspath: Boolean = false) {
    dependenciesFromClassloader(*libraries, wholeClasspath = wholeClasspath)
}

fun JvmScriptCompilationConfigurationBuilder.dependenciesFromClassloader(
    vararg libraries: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
) {
    ScriptCompilationConfiguration.dependencies.append(
        JvmDependency(scriptCompilationClasspathFromContext(*libraries, classLoader = classLoader, wholeClasspath = wholeClasspath))
    )
}

val JvmScriptCompilationConfigurationKeys.javaHome by PropertiesCollection.keyCopy(ScriptingHostConfiguration.jvm.javaHome)

@Suppress("unused")
val ScriptCompilationConfigurationKeys.jvm
    get() = JvmScriptCompilationConfigurationBuilder()

