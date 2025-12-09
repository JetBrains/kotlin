/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinSpecDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinWasmSpec
import org.jetbrains.kotlin.gradle.targets.wasm.spec.SpecExec
import org.jetbrains.kotlin.gradle.utils.withType
import javax.inject.Inject

@OptIn(ExperimentalWasmDsl::class)
abstract class KotlinSpecIr
@Inject
internal constructor(
    target: KotlinJsIrTarget,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
) :
    KotlinJsIrSubTarget(target, "specBLA"),
    KotlinSpecDsl {

    @Deprecated("Extending this class is deprecated. Scheduled for removal in Kotlin 2.4.")
    constructor(
        target: KotlinJsIrTarget,
    ) : this(
        target = target,
        objects = target.project.objects,
        providers = target.project.providers,
    )

//    private val spec = SpecPlugin.applyWithSpecEnv(project)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside d8 using the builtin test framework"

    override fun runTask(body: Action<SpecExec>) {
        subTargetConfigurators
            .withType<SpecEnvironmentConfigurator>()
            .configureEach {
                it.configureRun(body)
            }
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        test.testFramework = KotlinWasmSpec(test, objects, providers)
    }

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {}

}
