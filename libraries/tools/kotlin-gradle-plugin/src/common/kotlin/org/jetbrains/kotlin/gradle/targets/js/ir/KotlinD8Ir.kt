/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmD8Dsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmD8
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.domainObjectSet
import javax.inject.Inject

abstract class KotlinD8Ir @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "d8"),
    KotlinWasmD8Dsl {

    private val d8 = D8RootPlugin.apply(project.rootProject)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside d8 using the builtin test framework"

    override fun runTask(body: Action<D8Exec>) {
        subTargetConfigurators.configureEach {
            if (it is D8EnvironmentConfigurator) {
                it.configureRun(body)
            }
        }
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        test.testFramework = KotlinWasmD8(test)
    }

    override fun configureTestDependencies(test: KotlinJsTest) {
        test.dependsOn(d8.setupTaskProvider)
    }
}