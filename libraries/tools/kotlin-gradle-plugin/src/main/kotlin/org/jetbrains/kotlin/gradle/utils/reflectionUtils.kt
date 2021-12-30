/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.plugin.MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING

class IsolatedKotlinClasspathClassCastException : ClassCastException(MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING)

/**
 * Behaves like a regular cast function, but will be able to detect cast failures because
 * of an isolated classpath. In this case a more detailed error message will be emitted.
 *
 * ```
 * "".castIsolatedKotlinPluginClassLoaderAware<Int>() // fails like "" as Int
 * "".castIsolatedKotlinPluginClassLoaderAware<Int?>() // returns null like "" as? Int
 * ```
 * @return [this] as T if possible (regular cast)
 * @throws ClassCastException is not castable
 * @throws IsolatedKotlinClasspathClassCastException when a separated classpath is detected. See [MULTIPLE_KOTLIN_PLUGINS_LOADED_WARNING]
 */
internal inline fun <reified T> Any.castIsolatedKotlinPluginClassLoaderAware(): T {
    /* Fast path */
    if (this is T) return this

    val targetClassFromReceiverClassLoader = try {
        this::class.java.classLoader.loadClass(T::class.java.name)
    } catch (_: ClassNotFoundException) {
        null
    }

    /*
    User setup lead to Gradle separating the classpath between two projects applying the Kotlin Gradle plugin.
    This is not a supported setup and the ClassCastException should report this.
    */
    if (targetClassFromReceiverClassLoader != null && targetClassFromReceiverClassLoader != T::class.java) {
        /*
        We can be lenient if
        1) T is marked nullable (`null is T`), which implies a safe cast (behavior like `as?`)
        2) We know that the cast is impossible from the perspective of 'this'
         */
        if (null is T && !targetClassFromReceiverClassLoader.isInstance(this)) return null as T
        throw IsolatedKotlinClasspathClassCastException()
    }

    /* Will throw "regular" ClassCastException OR return null if T is marked nullable */
    return if (null is T) null as T
    else this as T
}
