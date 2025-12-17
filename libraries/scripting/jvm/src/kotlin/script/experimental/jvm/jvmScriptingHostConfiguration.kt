/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm

import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.host.*
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty
import kotlin.script.experimental.util.PropertiesCollection

interface JvmScriptingHostConfigurationKeys

open class JvmScriptingHostConfigurationBuilder : JvmScriptingHostConfigurationKeys, PropertiesCollection.Builder() {

    companion object : JvmScriptingHostConfigurationKeys
}

@Deprecated("Unused")
val JvmScriptingHostConfigurationKeys.javaHome by PropertiesCollection.key<File>(File(System.getProperty("java.home")))

val JvmScriptingHostConfigurationKeys.jdkHome by PropertiesCollection.key<File>()

val JvmScriptingHostConfigurationKeys.baseClassLoader by PropertiesCollection.key<ClassLoader>(
    {
        get(ScriptingHostConfiguration.configurationDependencies)?.let {
            URLClassLoader(it.toClassPathOrEmpty().map { f -> f.toURI().toURL() }.toTypedArray(), null)
        }
    },
    isTransient = true
)

@Suppress("unused")
val ScriptingHostConfigurationKeys.jvm
    get() = JvmScriptingHostConfigurationBuilder()

val defaultJvmScriptingHostConfiguration
    get() = ScriptingHostConfiguration {
        getScriptingClass(JvmGetScriptingClass())
    }

interface GetScriptingClassByClassLoader : GetScriptingClass {
    operator fun invoke(classType: KotlinType, contextClassLoader: ClassLoader?, hostConfiguration: ScriptingHostConfiguration): KClass<*>
}

class JvmGetScriptingClass : GetScriptingClassByClassLoader {

    @Transient
    private var classLoader: ClassLoader? = null

    override fun invoke(classType: KotlinType, contextClass: KClass<*>, hostConfiguration: ScriptingHostConfiguration): KClass<*> =
        invoke(classType, contextClass.java.classLoader, hostConfiguration)

    @Synchronized
    override operator fun invoke(
        classType: KotlinType,
        contextClassLoader: ClassLoader?,
        hostConfiguration: ScriptingHostConfiguration
    ): KClass<*> {

        // checking if class already loaded in the same context
        val fromClass = classType.fromClass
        if (fromClass != null) {
            if (fromClass.java.classLoader == null) return fromClass // root classloader
            val actualClassLoadersChain = generateSequence(contextClassLoader) { it.parent }
            if (actualClassLoadersChain.any { it == fromClass.java.classLoader }) return fromClass
        }

        if (classLoader == null) {
            classLoader = hostConfiguration[ScriptingHostConfiguration.jvm.baseClassLoader]
        }
        if (classLoader == null) {
            val dependencies = hostConfiguration[ScriptingHostConfiguration.configurationDependencies].toClassPathOrEmpty()
            if (dependencies.isNotEmpty())
                classLoader = URLClassLoader(dependencies.map { it.toURI().toURL() }.toTypedArray(), contextClassLoader)
        }

        return try {
            (classLoader ?: ClassLoader.getSystemClassLoader()).loadClass(classType.typeName).kotlin
        } catch (e: Throwable) {
            throw IllegalArgumentException("unable to load class ${classType.typeName}", e)
        }
    }
}
