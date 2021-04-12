/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object AndroidGradleWrapper {
    fun getRuntimeJars(basePlugin: BasePlugin<*>, baseExtension: BaseExtension): Any? {
        return basePlugin("getRuntimeJarList") ?: baseExtension("getBootClasspath") ?: basePlugin("getBootClasspath")
    }
}

private operator fun Any?.invoke(methodName: String): Any? {
    if (this == null) {
        return null
    }

    fun Array<Method>.findMethod() =
        singleOrNull { it.name == methodName && it.parameterCount == 0 && !Modifier.isStatic(it.modifiers) }

    val clazz = this::class.java
    val methodToInvoke = clazz.declaredMethods.findMethod() ?: clazz.methods.findMethod() ?: return null
    val oldIsAccessible = methodToInvoke.isAccessible

    try {
        methodToInvoke.isAccessible = true
        return methodToInvoke.invoke(this)
    } catch (e: InvocationTargetException) {
        throw e.targetException
    } catch (e: Throwable) {
        throw RuntimeException("Can't invoke method '$methodName()' on $this", e)
    } finally {
        methodToInvoke.isAccessible = oldIsAccessible
    }
}