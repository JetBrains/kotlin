/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.util.PropertiesCollection

interface JvmScriptingHostConfigurationKeys

open class JvmScriptingHostConfigurationBuilder : JvmScriptingHostConfigurationKeys, PropertiesCollection.Builder() {

    companion object : JvmScriptingHostConfigurationKeys
}

val JvmScriptingHostConfigurationKeys.javaHome by PropertiesCollection.key<File>(File(System.getProperty("java.home")))

@Suppress("unused")
val ScriptingHostConfigurationKeys.jvm
    get() = JvmScriptingHostConfigurationBuilder()

val defaultJvmScriptingHostConfiguration = ScriptingHostConfiguration {
    getScriptingClass(JvmGetScriptingClass())
}

class JvmGetScriptingClass : GetScriptingClass {

    private var dependencies: List<ScriptDependency>? = null
    private var classLoader: ClassLoader? = null
    private var baseClassLoaderIsInitialized = false
    private var baseClassLoader: ClassLoader? = null

    @Synchronized
    override fun invoke(classType: KotlinType, contextClass: KClass<*>, hostConfiguration: ScriptingHostConfiguration): KClass<*> {

        // checking if class already loaded in the same context
        val contextClassloader = contextClass.java.classLoader
        val fromClass = classType.fromClass
        if (fromClass != null) {
            if (fromClass.java.classLoader == null) return fromClass // root classloader
            val actualClassLoadersChain = generateSequence(contextClassloader) { it.parent }
            if (actualClassLoadersChain.any { it == fromClass.java.classLoader }) return fromClass
        }

        val newDeps = hostConfiguration[ScriptingHostConfiguration.configurationDependencies]
        if (dependencies == null) {
            dependencies = newDeps
        } else {
            if (newDeps != dependencies) throw IllegalArgumentException("scripting configuration dependencies changed")
        }

        if (!baseClassLoaderIsInitialized) {
            baseClassLoader = contextClassloader
            baseClassLoaderIsInitialized = true
        }
        // TODO: this check breaks testLazyScriptDefinition, find out the reason and fix
//        else if (baseClassLoader != null) {
//            val baseClassLoadersChain = generateSequence(baseClassLoader) { it.parent }
//            if (baseClassLoadersChain.none { it == contextClassloader }) throw IllegalArgumentException("scripting class instantiation context changed")
//        }

        if (classLoader == null) {
            val classpath = dependencies?.flatMap { dependency ->
                when (dependency) {
                    is JvmDependency -> dependency.classpath.map { it.toURI().toURL() }
                    else -> throw IllegalArgumentException("unknown dependency type $dependency")
                }
            }
            classLoader =
                    if (classpath == null || classpath.isEmpty()) baseClassLoader
                    else URLClassLoader(classpath.toTypedArray(), baseClassLoader)
        }

        return try {
            classLoader!!.loadClass(classType.typeName).kotlin
        } catch (e: Throwable) {
            throw IllegalArgumentException("unable to load class $classType", e)
        }
    }
}
