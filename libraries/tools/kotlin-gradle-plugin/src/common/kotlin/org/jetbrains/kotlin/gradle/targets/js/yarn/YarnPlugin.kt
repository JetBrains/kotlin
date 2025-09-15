/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.web.yarn.CommonYarnPlugin

/**
 * A class representing the JavaScript specific implementation of the Yarn plugin.
 * The `YarnPlugin` applies configurations, tasks, and extensions necessary for managing
 * Yarn as a package manager within JS-targeted projects using Gradle.
 */
open class YarnPlugin : CommonYarnPlugin {
    override fun apply(target: Project) {
        YarnPluginApplier(
            platformDisambiguate = JsPlatformDisambiguator,
            yarnRootKlass = YarnRootExtension::class,
            yarnRootName = YarnRootExtension.YARN,
            yarnEnvSpecKlass = YarnRootEnvSpec::class,
            yarnEnvSpecName = YarnRootEnvSpec.YARN,
            nodeJsRootApply = { NodeJsRootPlugin.apply(it) },
            nodeJsRootExtension = { it.kotlinNodeJsRootExtension },
            nodeJsEnvSpec = { it.kotlinNodeJsEnvSpec },
            lockFileDirectory = { it.resolve(LockCopyTask.KOTLIN_JS_STORE) },
        ).apply(target)
    }


    companion object {
        fun apply(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YarnRootExtension.YARN) as YarnRootExtension
        }

        @InternalKotlinGradlePluginApi
        const val STORE_YARN_LOCK_BASE_NAME = "storeYarnLock"

        @InternalKotlinGradlePluginApi
        const val RESTORE_YARN_LOCK_BASE_NAME = "restoreYarnLock"

        @InternalKotlinGradlePluginApi
        const val UPGRADE_YARN_LOCK_BASE_NAME = "upgradeYarnLock"

        @Deprecated(
            "Use storeYarnLockTaskProvider from YarnRootExtension or WasmYarnRootExtension instead. " +
                    "Scheduled for removal in Kotlin 2.4.",
            level = DeprecationLevel.ERROR
        )
        const val STORE_YARN_LOCK_NAME = "kotlinStoreYarnLock"

        @Deprecated(
            "Use restoreYarnLockTaskProvider from YarnRootExtension or WasmYarnRootExtension instead. " +
                    "Scheduled for removal in Kotlin 2.4.",
            level = DeprecationLevel.ERROR
        )
        const val RESTORE_YARN_LOCK_NAME = "kotlinRestoreYarnLock"

        @Deprecated(
            "It is task name for JS target only. Use UPGRADE_YARN_LOCK_BASE_NAME to calculate correct name for your platform. " +
                    "Scheduled for removal in Kotlin 2.4.",
            level = DeprecationLevel.ERROR
        )
        const val UPGRADE_YARN_LOCK = "kotlinUpgradeYarnLock"

        @InternalKotlinGradlePluginApi
        fun yarnLockMismatchMessage(upgradeTaskName: String) =
            "Lock file was changed. Run the `$upgradeTaskName` task to actualize lock file"
    }
}
