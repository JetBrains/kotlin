/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.kapt3.base.KaptOptions
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

class KaptOptionsProvider(private val testServices: TestServices) : TestService {
    private val cache: MutableMap<TestModule, KaptOptions> = mutableMapOf()

    fun registerKaptOptions(module: TestModule, builder: KaptOptions.Builder.() -> Unit) {
        if (module in cache) {
            testServices.assertions.fail { "KaptOptions for module $module already registered" }
        }
        val options = KaptOptions.Builder().apply(builder).build()
        cache[module] = options
    }

    operator fun get(module: TestModule): KaptOptions {
        return cache.getValue(module)
    }
}

val TestServices.kaptOptionsProvider: KaptOptionsProvider by TestServices.testServiceAccessor()
