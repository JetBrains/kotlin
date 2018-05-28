/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*

class JvmGetScriptingClass : GetScriptingClass {

    private var dependencies: List<ScriptDependency>? = null
    private var classLoader: ClassLoader? = null
    private var baseClassLoaderIsInitialized = false
    private var baseClassLoader: ClassLoader? = null

    @Synchronized
    override fun invoke(classType: KotlinType, contextClass: KClass<*>, environment: ScriptingEnvironment): KClass<*> {

        // checking if class already loaded in the same context
        val contextClassloader = contextClass.java.classLoader
        val fromClass = classType.fromClass
        if (fromClass != null) {
            if (fromClass.java.classLoader == null) return fromClass // root classloader
            val actualClassLoadersChain = generateSequence(contextClassloader) { it.parent }
            if (actualClassLoadersChain.any { it == fromClass.java.classLoader }) return fromClass
        }

        val newDeps = environment.getOrNull(ScriptingEnvironmentProperties.configurationDependencies)
        if (dependencies == null) {
            dependencies = newDeps
        } else {
            if (newDeps != dependencies) throw IllegalArgumentException("scripting environment dependencies changed")
        }

        if (!baseClassLoaderIsInitialized) {
            baseClassLoader = contextClassloader
            baseClassLoaderIsInitialized = true
        } else if (baseClassLoader != null) {
            val baseClassLoadersChain = generateSequence(baseClassLoader) { it.parent }
            if (baseClassLoadersChain.none { it == contextClassloader }) throw IllegalArgumentException("scripting class instantiation context changed")
        }

        if (classLoader == null) {
            val classpath = dependencies?.flatMap {
                when(it) {
                    is JvmDependency -> it.classpath.map { it.toURI().toURL() }
                    else -> throw IllegalArgumentException("unknown dependency type $it")
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
