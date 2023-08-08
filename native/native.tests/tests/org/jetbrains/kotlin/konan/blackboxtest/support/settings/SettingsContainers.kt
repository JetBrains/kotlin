/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import kotlin.reflect.KClass

internal abstract class Settings(private val parent: Settings?, settings: Iterable<Any>) {
    private val map: Map<KClass<*>, Any> = buildMap {
        settings.forEach {
            val settingClass: KClass<*>
            val setting: Any
            if (it is Pair<*, *>) {
                settingClass = it.first as KClass<*>
                setting = it.second ?: error("Setting $settingClass is null")
            } else {
                settingClass = it::class
                setting = it
            }

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
 * The hierarchy of settings containers for Native black box tests:
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

/**
 * The hierarchy of settings containers for simple Native tests (e.g. KLIB tests):
 *
 * | Settings container        | Parent                    | Scope                                       |
 * | ------------------------- | ------------------------- | ------------------------------------------- |
 * | [TestProcessSettings]     | `null`                    | The whole Gradle test executor process      |
 * | [SimpleTestClassSettings] | [TestProcessSettings]     | The single top-level (enclosing) test class |
 * | [SimpleTestRunSettings]   | [SimpleTestClassSettings] | The single test run of a test function      |
 */
internal class SimpleTestClassSettings(parent: TestProcessSettings, settings: Iterable<Any>) : Settings(parent, settings)
internal class SimpleTestRunSettings(parent: SimpleTestClassSettings, settings: Iterable<Any>) : Settings(parent, settings)
