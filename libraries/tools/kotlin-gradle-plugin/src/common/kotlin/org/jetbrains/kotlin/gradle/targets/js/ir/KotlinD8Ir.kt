/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmD8Dsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmD8
import org.jetbrains.kotlin.gradle.utils.withType
import javax.inject.Inject

@OptIn(ExperimentalWasmDsl::class)
abstract class KotlinD8Ir @Inject constructor(target: KotlinJsIrTarget) :
    KotlinJsIrSubTarget(target, "d8"),
    KotlinWasmD8Dsl {

    private val d8 = D8Plugin.applyWithEnvSpec(project)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside d8 using the builtin test framework"

    override fun runTask(body: Action<D8Exec>) {
        subTargetConfigurators
            .withType<D8EnvironmentConfigurator>()
            .configureEach {
                it.configureRun(body)
            }
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        test.testFramework = KotlinWasmD8(test)
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        with(d8) {
            test.dependsOn(project.d8SetupTaskProvider)
        }
    }

}