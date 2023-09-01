/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.getOrPut
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * ### Generic mechanism of attaching 'data' to [Project]
 * #### e.g. attaching a simple property to a [Project]
 * ```kotlin
 * class Foo(val projectName: String)
 *
 * val Project.myFoo by lazyProjectProperty {
 *     Foo(project.name)
 * }
 * ```
 *
 * _Usage in Project 'a'_
 *
 * ```kotlin
 * class MyPlugin : Plugin<Project> {
 *     fun apply(project: Project) {
 *         // prints 'Foo("a")'
 *         println(project.myFoo)
 *     }
 * }
 * ```
 * _Usage in Project 'b'_
 *
 * ```kotlin
 * class MyPlugin : Plugin<Project> {
 *     fun apply(project: Project) {
 *         // prints 'Foo("b")'
 *         println(project.myFoo)
 *     }
 * }
 * ```
 *
 * ### Note:
 * The key used for storing the property to the [Project] is the instance of the returned [ReadOnlyProperty],
 * *not* the type, or any String based key
 *
 */
internal fun <T : Any> lazyProjectProperty(initializer: Project.() -> T): ReadOnlyProperty<Project, T> = LazyProperty(
    storage = { lazyPropertyStorage },
    initializer = initializer
)

/**
 * Same as [lazyProjectProperty], but will allow storing the property on any object implementing [HasMutableExtras]
 */
internal fun <R : HasMutableExtras, T : Any> lazyExtrasProperty(initializer: R.() -> T): ReadOnlyProperty<R, T> = LazyProperty(
    storage = { lazyPropertyStorage },
    initializer = initializer
)

private class LazyProperty<in T, out V>(
    private val storage: T.() -> LazyPropertyStorage,
    private val initializer: T.() -> V,
) : ReadOnlyProperty<T, V> {
    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return thisRef.storage().getOrPut(this) { thisRef.initializer() }
    }
}

private class LazyPropertyStorage {
    private val values = WeakHashMap<LazyProperty<*, *>, Any?>()
    fun <T> getOrPut(key: LazyProperty<*, T>, create: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return values.getOrPut(key) { create() } as T
    }
}

private val Project.lazyPropertyStorage
    get() = extraProperties.getOrPut(LazyPropertyStorage::class.java.name) { LazyPropertyStorage() }

private val lazyPropertyStorageExtrasKey = extrasKeyOf<LazyPropertyStorage>()

private val HasMutableExtras.lazyPropertyStorage
    get() = extras.getOrPut(lazyPropertyStorageExtrasKey) { LazyPropertyStorage() }
