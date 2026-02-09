/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.isCurrentHost
import org.jetbrains.kotlin.gradle.targets.KotlinTestRunFactory
import org.jetbrains.kotlin.gradle.targets.native.internal.XcodeDefaultTestDevicesValueSource
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class KotlinNativeHostTestRunFactory(private val target: KotlinNativeTarget) : KotlinTestRunFactory<KotlinNativeHostTestRun> {
    override fun create(name: String): KotlinNativeHostTestRun {
        val project = target.project
        val konanTarget = target.konanTarget
        val isCurrentHost = konanTarget.isCurrentHost
        return DefaultHostTestRun(name, target).apply {
            // Report diagnostic during test run creation (not in task configuration) to ensure it's emitted
            // even with configuration avoidance when the task is not realized
            project.reportDisabledNativeTargetTaskWarningIfNeeded(
                isEnabledOnCurrentHost = isCurrentHost,
                taskName = testTaskName,
                targetName = konanTarget.name,
                currentHost = HostManager.hostOrNull?.name ?: "unsupported",
                reason = "tests can only run on ${konanTarget.name}",
            )
            executionTask = this@KotlinNativeHostTestRunFactory.target.registerNativeTestTask(testTaskName, isCurrentHost)
            setExecutionSourceFrom(this@KotlinNativeHostTestRunFactory.target.binaries.getTest(NativeBuildType.DEBUG))
            project.kotlinTestRegistry.registerTestTask(executionTask)
        }
    }
}

internal class KotlinNativeSimulatorTestRunFactory(private val target: KotlinNativeTarget) :
    KotlinTestRunFactory<KotlinNativeSimulatorTestRun> {
    override fun create(name: String): KotlinNativeSimulatorTestRun {
        val thisTarget = target
        return DefaultSimulatorTestRun(name, target).apply {
            val project = target.project
            val isCompatibleHost = HostManager.hostIsMac && HostManager.host.architecture == thisTarget.konanTarget.architecture
            // Report diagnostic during test run creation (not in task configuration) to ensure it's emitted
            // even with configuration avoidance when the task is not realized
            val reason = when {
                !HostManager.hostIsMac -> "simulator tests require macOS"
                else -> "architecture mismatch - target requires ${thisTarget.konanTarget.architecture.name}, current host is ${HostManager.host.architecture.name}"
            }
            project.reportDisabledNativeTargetTaskWarningIfNeeded(
                isEnabledOnCurrentHost = isCompatibleHost,
                taskName = testTaskName,
                targetName = thisTarget.konanTarget.name,
                currentHost = HostManager.platformName(),
                reason = reason,
            )
            executionTask = thisTarget.registerNativeTestTask<KotlinNativeSimulatorTest>(testTaskName, isCompatibleHost) { testTask ->
                testTask.configureDeviceId(thisTarget.konanTarget)
                testTask.standalone.convention(true).finalizeValueOnRead()
            }
            setExecutionSourceFrom(thisTarget.binaries.getTest(NativeBuildType.DEBUG))
            project.kotlinTestRegistry.registerTestTask(executionTask)
        }
    }
}

private inline fun <reified T : KotlinNativeTest> KotlinNativeTarget.registerNativeTestTask(
    name: String,
    isEnabledOnCurrentHost: Boolean,
    crossinline configure: (T) -> Unit = {},
): TaskProvider<T> {
    return project.registerTask(name, T::class.java) { testTask ->
        testTask.group = LifecycleBasePlugin.VERIFICATION_GROUP
        testTask.description = "Executes Kotlin/Native unit tests for target ${targetName}."
        testTask.targetName = this@registerNativeTestTask.targetName
        testTask.onlyIf("Tests require compatible host") { isEnabledOnCurrentHost }
        testTask.workingDir = project.projectDir.absolutePath
        testTask.configureConventions()
        configure(testTask)
    }
}

private fun KotlinNativeSimulatorTest.configureDeviceId(konanTarget: KonanTarget) {
    if (!isEnabled) return
    val deviceIdProvider = project.providers.of(XcodeDefaultTestDevicesValueSource::class.java) {}

    // Extract primitive values to avoid [target] capture in lambda
    val konanTargetFamily = konanTarget.family
    val konanTargetName = konanTarget.name

    val defaultDevice = deviceIdProvider.map {
        it[konanTargetFamily]
            ?: error("Xcode does not support simulator tests for ${konanTargetName}. Check that requested SDK is installed.")
    }
    device.convention(defaultDevice).finalizeValueOnRead()
}
