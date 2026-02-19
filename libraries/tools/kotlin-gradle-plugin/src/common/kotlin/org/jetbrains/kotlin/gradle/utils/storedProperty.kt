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
 * val Project.myFoo by projectStoredProperty {
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
internal fun <T> projectStoredProperty(initializer: Project.() -> T): ReadOnlyProperty<Project, T> = StoredLazyProperty(
    storage = { storedPropertyStorage },
    initializer = initializer
)

/**
 * Same as [projectStoredProperty], but will allow storing the property on any object implementing [HasMutableExtras]
 */
internal fun <R : HasMutableExtras, T> extrasStoredProperty(initializer: R.() -> T): ReadOnlyProperty<R, T> = StoredLazyProperty(
    storage = { storedPropertyStorage },
    initializer = initializer
)


private interface StoredProperty<out T>

private class StoredLazyProperty<in T, out V>(
    private val storage: T.() -> StoredPropertyStorage,
    private val initializer: T.() -> V,
) : ReadOnlyProperty<T, V>, StoredProperty<V> {
    override fun getValue(thisRef: T, property: KProperty<*>): V {
        return thisRef.storage().getOrPut(this) { thisRef.initializer() }
    }
}

private class StoredPropertyStorage {
    /**
     * The stored value of the [StoredProperty] will be kept as long as the reference to the [StoredProperty] is alive.
     * e.g. a top level delegate like
     * ```kotlin
     * val Project.myProperty by storedProjectProperty { 42 }
     * ```
     *
     * will be effectively kept forever as the [StoredProperty] is referenced globally from the static scope
     * of the containing Source File.
     *
     * However:
     *
     * ```kotlin
     * class MyClass {
     *     val Project.myProperty by storedProjectProperty { 42 }
     * }
     *
     * val myInstance = MyClass()
     * ```
     *
     * Such code would keep the property referenced from `myInstance`. Once this `myInstance` is collected,
     * there is no need for keeping the attached/stored property. `myInstance` getting collected leads to `myPropert$storedProjectProperty`
     * getting collected and the associated value will be released
     */
    private val values = WeakHashMap<StoredProperty<*>, Any?>()
    fun <T> getOrPut(key: StoredLazyProperty<*, T>, create: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return if (key in values) values[key] as T
        else values.getOrPut(key, create) as T
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: StoredProperty<T>): T? {
        return values[key] as T?
    }
}

private val Project.storedPropertyStorage
    get() = extraProperties.getOrPut(StoredPropertyStorage::class.java.name) { StoredPropertyStorage() }

private val storedPropertyStorageExtrasKey = extrasKeyOf<StoredPropertyStorage>()

private val HasMutableExtras.storedPropertyStorage
    get() = extras.getOrPut(storedPropertyStorageExtrasKey) { StoredPropertyStorage() }
