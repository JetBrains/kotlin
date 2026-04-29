/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.TASKS_GROUP_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask.Companion.NPM_TOOLING_DIR_NAME
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.nodejs.CommonNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsRootPluginApplier
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NpmToolingEnv
import org.jetbrains.kotlin.gradle.targets.web.nodejs.configureNodeJsEnvironmentTasks
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.userKotlinPersistentDir

/**
 * Represents the root plugin class for configuring and applying Node.js-related functionality specifically tailored for WebAssembly (Wasm) projects.
 * This plugin is responsible for setting up Node.js as a runtime or development environment for Wasm projects, leveraging specific tools
 * and extensions required for Wasm builds.
 *
 * This plugin extends from the `CommonNodeJsRootPlugin` interface, providing Wasm-specific customizations for Node.js integration.
 *
 * This class manages:
 * - Applying specific Node.js configuration tailored for Wasm projects.
 * - Registering Wasm-specific Node.js root extensions, like `WasmNodeJsRootExtension` and `WasmNpmExtension`.
 * - Configuring dependencies and lockfile management for Wasm project setups.
 *
 */
abstract class WasmNodeJsRootPlugin internal constructor() : CommonNodeJsRootPlugin {

    override fun apply(target: Project) {
        val rootDirectoryName = WasmPlatformDisambiguator.platformDisambiguator
        val nodeJsRootPluginApplier = NodeJsRootPluginApplier(
            platformDisambiguate = WasmPlatformDisambiguator,
            nodeJsRootKlass = WasmNodeJsRootExtension::class,
            nodeJsRootName = WasmNodeJsRootExtension.EXTENSION_NAME,
            npmKlass = WasmNpmExtension::class,
            npmName = WasmNpmExtension.Companion.EXTENSION_NAME,
            rootDirectoryName = rootDirectoryName,
            lockFileDirectory = { it.dir(LockCopyTask.Companion.KOTLIN_JS_STORE).dir(rootDirectoryName) },
            singleNodeJsPluginApply = { WasmNodeJsPlugin.apply(it) },
            yarnPlugin = WasmYarnPlugin::class,
            beforePackageManager = { rootProject ->
                kotlinToolingSetup(rootProject)
            },
            platformType = KotlinPlatformType.wasm,
        )

        nodeJsRootPluginApplier.apply(target)
    }

    private fun kotlinToolingSetup(
        target: Project,
    ) {
        val nodeJsRoot = target.extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME) as WasmNodeJsRootExtension

        @Suppress("DEPRECATION_ERROR")
        nodeJsRoot.version = "25.0.0"

        val nodeJs = target.extensions.getByName(WasmNodeJsEnvSpec.EXTENSION_NAME) as WasmNodeJsEnvSpec

        val packageManagerName = nodeJsRoot.packageManagerExtension.map { it.name }

        val allDeps = nodeJsRoot.versions.allDependencies

        val wasmNpmToolingExtension = createWasmNpmToolingExtension(target, packageManagerName, allDeps)
        val npmTooling = wasmNpmToolingExtension.produceEnv()

        registerKotlinToolingSetupTask(target, npmTooling, allDeps, nodeJsRoot, nodeJs)

        nodeJsRoot.npmTooling.value(npmTooling)
    }

    private fun createWasmNpmToolingExtension(
        target: Project,
        packageManagerName: Provider<String>,
        allDeps: List<NpmPackageVersion>,
    ): WasmNpmTooling {
        val extension = target.extensions.create(
            "wasmNpmTooling",
            WasmNpmTooling::class.java
        )
        extension.allDeps.set(allDeps)
        extension.installationDir.convention(
            target.objects.directoryProperty()
                .fileValue(target.userKotlinPersistentDir.resolve(NPM_TOOLING_DIR_NAME))
                .zip(packageManagerName) { toolingDir, name ->
                    toolingDir.dir(name)
                }
        )
        return extension
    }

    private fun registerKotlinToolingSetupTask(
        target: Project,
        npmTooling: Provider<NpmToolingEnv>,
        allDeps: List<NpmPackageVersion>,
        nodeJsRoot: WasmNodeJsRootExtension,
        nodeJs: WasmNodeJsEnvSpec,
    ) {
        target.registerTask<KotlinToolingSetupTask>(extensionName(KotlinToolingSetupTask.BASE_NAME)) { task ->
            task.onlyIf("Output directory is set explicitly. All installed dependencies are expected in the output directory.") {
                !npmTooling.get().explicitDir
            }

            task.versionsHash
                .value(npmTooling.map { it.version })
                .disallowChanges()

            task.tools
                .value(allDeps)
                .disallowChanges()

            task.destination
                .fileProvider(npmTooling.map { it.dir })
                .disallowChanges()

            task.nodeModules
                .fileProvider(npmTooling.map { it.dir.resolve("node_modules") })
                .disallowChanges()

            task.configureNodeJsEnvironmentTasks(
                nodeJsRoot,
                nodeJs
            )

            with(nodeJs) {
                task.dependsOn(target.nodeJsSetupTaskProvider)
            }
            task.group = TASKS_GROUP_NAME
            task.description = "Find, download and link NPM dependencies and projects"

            task.outputs.upToDateWhen {
                task.nodeModules.getFile().exists()
            }
        }
    }

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        fun apply(rootProject: Project): WasmNodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(WasmNodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME) as WasmNodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: WasmNodeJsRootExtension
            get() = extensions.getByName(WasmNodeJsRootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    extensionName(
                        KotlinNpmResolutionManager::class.java.name,
                        prefix = null,
                    ),
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }
    }
}
