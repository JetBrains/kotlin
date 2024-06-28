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
import org.jetbrains.kotlin.gradle.utils.XcodeUtils
import org.jetbrains.kotlin.gradle.utils.valueSourceWithExecProviderCompat
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class KotlinNativeHostTestRunFactory(private val target: KotlinNativeTarget) : KotlinTestRunFactory<KotlinNativeHostTestRun> {
    override fun create(name: String): KotlinNativeHostTestRun {
        return DefaultHostTestRun(name, target).apply {
            val project = target.project
            executionTask = this@KotlinNativeHostTestRunFactory.target.registerNativeTestTask(testTaskName)
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
            executionTask = thisTarget.registerNativeTestTask<KotlinNativeSimulatorTest>(testTaskName) { testTask ->
                testTask.isEnabled = HostManager.hostIsMac && HostManager.host.architecture == thisTarget.konanTarget.architecture
                testTask.configureDeviceId(thisTarget.konanTarget)
                testTask.standalone.convention(true).finalizeValueOnRead()
            }
            setExecutionSourceFrom(thisTarget.binaries.getTest(NativeBuildType.DEBUG))
            project.kotlinTestRegistry.registerTestTask(executionTask)
        }
    }
}

private inline fun <reified T : KotlinNativeTest> KotlinNativeTarget.registerNativeTestTask(
    name: String, crossinline configure: (T) -> Unit = {},
): TaskProvider<T> {
    return project.registerTask(name, T::class.java) { testTask ->
        testTask.group = LifecycleBasePlugin.VERIFICATION_GROUP
        testTask.description = "Executes Kotlin/Native unit tests for target ${targetName}."
        testTask.targetName = this@registerNativeTestTask.targetName
        testTask.enabled = konanTarget.isCurrentHost
        testTask.workingDir = project.projectDir.absolutePath
        testTask.configureConventions()
        configure(testTask)
    }
}

private fun KotlinNativeSimulatorTest.configureDeviceId(konanTarget: KonanTarget) {
    if (!isEnabled) return
    val deviceIdProvider = project.valueSourceWithExecProviderCompat(XcodeDefaultTestDevicesValueSource::class.java)

    // Extract primitive values to avoid [target] capture in lambda
    val konanTargetFamily = konanTarget.family
    val konanTargetName = konanTarget.name

    val defaultDevice = deviceIdProvider.map {
        it[konanTargetFamily]
            ?: error("Xcode does not support simulator tests for ${konanTargetName}. Check that requested SDK is installed.")
    }
    device.convention(defaultDevice).finalizeValueOnRead()
}