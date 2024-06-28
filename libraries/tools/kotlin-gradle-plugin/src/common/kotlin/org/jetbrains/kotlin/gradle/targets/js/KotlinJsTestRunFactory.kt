/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.jetbrains.kotlin.gradle.targets.KotlinTestRunFactory
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName

internal class KotlinJsTestRunFactory(private val target: KotlinJsIrTarget) : KotlinTestRunFactory<KotlinJsReportAggregatingTestRun> {
    override fun create(name: String): KotlinJsReportAggregatingTestRun {
        val testRun = target.project.objects.newInstance(KotlinJsReportAggregatingTestRun::class.java, name, target)
        val testTask = target.project.kotlinTestRegistry.getOrCreateAggregatedTestTask(
            name = testRun.testTaskName,
            description = "Run JS tests for all platforms"
        )

        // workaround to avoid the infinite recursion in item factories of the target and the subtargets:
        target.testRuns.matching { it.name == name }.whenObjectAdded {
            it.configureAllExecutions {
                // do not do anything with the aggregated test run, but ensure that they are created
            }
        }

        testRun.executionTask = testTask
        return testRun
    }
}