/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsGenericDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import javax.inject.Inject

abstract class KotlinGenericJsIr
@Inject
internal constructor(
    target: KotlinJsIrTarget,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory
) :
    KotlinJsIrSubTarget(target, "generic"),
    KotlinJsGenericDsl {

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests in a generic environment using the builtin test framework"

    @ExperimentalMainFunctionArgumentsDsl
    override fun passProcessArgvToMainFunction() {
        target.passAsArgumentToMainFunction("process.argv")
    }

    @ExperimentalMainFunctionArgumentsDsl
    override fun passCliArgumentsToMainFunction() {
        target.passAsArgumentToMainFunction("process.argv.slice(2)")
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
    }
}
