/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunner
import org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import javax.inject.Inject

@CacheableTask
internal abstract class KotlinJsIrLinkWithWorkers
@Inject
constructor(
    objectFactory: ObjectFactory,
    workerExecutor: WorkerExecutor
) : KotlinJsIrLink(objectFactory) {
    override val compilerRunner: Provider<GradleCompilerRunner> =
        objects.propertyWithConvention(
            gradleCompileTaskProvider.map {
                GradleCompilerRunnerWithWorkers(
                    it,
                    null,
                    workerExecutor
                ) as GradleCompilerRunner
            }
        )
}