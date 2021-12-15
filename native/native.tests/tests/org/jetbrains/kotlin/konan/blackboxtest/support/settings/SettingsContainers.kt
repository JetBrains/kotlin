/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import gnu.trove.THashMap
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.reflect.KClass

internal abstract class Settings(private val parent: Settings?, settings: Iterable<Any>) {
    private val map: Map<KClass<*>, Any> = THashMap<KClass<*>, Any>().apply {
        settings.forEach { it ->
            val (settingClass: KClass<*>, setting: Any) = if (it is Pair<*, *>) it.cast() else it::class to it
            val previous = put(settingClass, setting)
            assertTrue(previous == null) { "Duplicated settings: $settingClass, $previous, $setting" }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: KClass<out T>): T = map[clazz] as T?
        ?: parent?.get(clazz)
        ?: fail { "No such setting: $clazz" }

    inline fun <reified T : Any> get(): T = get(T::class)
}

/**
 * The hierarchy of settings containers:
 *
 * | Settings container    | Parent                 | Scope                                       |
 * | --------------------- | ---------------------- | ------------------------------------------- |
 * | [TestProcessSettings] | `null`                 | The whole Gradle test executor process      |
 * | [TestClassSettings]   | [TestProcessSettings]  | The single top-level (enclosing) test class |
 * | [TestRunSettings]     | [TestClassSettings]    | The single test run of a test function      |
 */
internal class TestProcessSettings(vararg settings: Any) : Settings(parent = null, settings.asIterable())
internal class TestClassSettings(parent: TestProcessSettings, settings: Iterable<Any>) : Settings(parent, settings)
internal class TestRunSettings(parent: TestClassSettings, settings: Iterable<Any>) : Settings(parent, settings)
