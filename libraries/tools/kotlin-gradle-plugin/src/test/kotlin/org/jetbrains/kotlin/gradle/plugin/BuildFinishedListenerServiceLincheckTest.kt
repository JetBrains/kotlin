/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import kotlin.test.fail
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test

class BuildFinishedListenerServiceLincheckTest {
    private val service = object : BuildFinishedListenerService() {
        override fun getParameters() = fail("The service has no parameters")
    }
    private val registeredActions = ConcurrentLinkedQueue<String>()

    @Operation
    fun registerAction(actionName: String) = service.onClose(object : () -> Unit {
        override fun invoke() {
            registeredActions.add(actionName)
        }

        override fun toString() = actionName
    })

    @Operation
    fun registerOnceByKey(key: String, actionName: String) = service.onCloseOnceByKey(key, object : () -> Unit {
        override fun invoke() {
            registeredActions.add(actionName)
        }

        override fun toString() = actionName
    })

    @Operation
    fun getRegisteredActions(): String {
        service.close()
        return registeredActions.joinToString()
    }

    @Test
    fun stressTest() = StressOptions().check(this::class)

    @Test
    fun modelCheckingTest() = ModelCheckingOptions().check(this::class)
}