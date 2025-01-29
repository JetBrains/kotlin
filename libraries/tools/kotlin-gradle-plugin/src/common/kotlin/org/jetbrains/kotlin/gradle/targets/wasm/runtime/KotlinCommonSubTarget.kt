/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.targets.wasm.runtime

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import javax.inject.Inject

abstract class KotlinCommonSubTarget
@Inject
constructor(
    target: KotlinJsIrTarget,
    name: String,
    val version: String,
) : KotlinJsIrSubTarget(target, name) {
    val os: Provider<String> = project.providers.systemProperty("os.name")
    val arch: Provider<String> = project.providers.systemProperty("os.arch")

    val envSpec: CommonEnvSpec = project.extensions.create(
        "${name}EnvSpec",
        CommonEnvSpec::class.java,
        name,
    ).apply {
        installationDirectory.convention(
            project.objects.directoryProperty().fileValue(
                project.gradle.gradleUserHomeDir.resolve("wasm-tools")
            )
        )

        download.convention(false)

        version.convention(this@KotlinCommonSubTarget.version)
    }

    val setupTask: TaskProvider<CommonSetupTask> = project.tasks.register(
        "kotlin${name.capitalized()}Setup",
        CommonSetupTask::class.java,
        envSpec
    ).also {
        it.configure {
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
            it.archiveOperation.set(this@KotlinCommonSubTarget.archiveOperation)
            it.os.set(this@KotlinCommonSubTarget.os)
            it.arch.set(this@KotlinCommonSubTarget.arch)
            it.version.set(this@KotlinCommonSubTarget.version)
        }
    }

    val runArgs: Property<ArgsProvider> = project.objects
        .property(ArgsProvider::class.java)
        .convention(ArgsProvider { _, _ ->
            emptyList()
        })

    val testArgs: Property<ArgsProvider> = project.objects
        .property(ArgsProvider::class.java)
        .convention(ArgsProvider { _, _ ->
            emptyList()
        })

    val archiveOperation: Property<ArchiveOperationsProvider> = project.objects.property(ArchiveOperationsProvider::class.java)

    override val testTaskDescription: String
        get() = "Run all ${target.name} tests inside $name using the builtin test framework"

    override fun configureTestDependencies(test: KotlinJsTest, binary: JsIrBinary) {
        test.dependsOn(binary.linkTask)
        test.dependsOn(setupTask)
    }

    override fun configureDefaultTestFramework(test: KotlinJsTest) {
        test.testFramework = CommonKotlinWasmTestFramework(
            test,
            name,
            target.project.objects,
            target.project.providers,
        ).apply {
            executable.set(envSpec.executable)
            argsProperty.set(this@KotlinCommonSubTarget.testArgs)
        }
    }
}