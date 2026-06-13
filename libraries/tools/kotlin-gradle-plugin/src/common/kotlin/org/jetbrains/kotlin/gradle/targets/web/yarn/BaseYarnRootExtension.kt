/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.yarn

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.AbstractSettings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnv
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NpmApiExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.Platform
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.*
import org.jetbrains.kotlin.gradle.targets.web.nodejs.BaseNodeJsRootExtension
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.internal.KgpBuildConstants
import java.io.File

/**
 * Common configuration for Yarn in a Kotlin/JS or Kotlin/WasmJS project.
 */
abstract class BaseYarnRootExtension internal constructor(
    val project: Project,
    val nodeJsRoot: BaseNodeJsRootExtension,
    private val yarnSpec: BaseYarnRootEnvSpec,
    private val objects: ObjectFactory,
    providers: ProviderFactory,
    private val execOps: ExecOperations,
) : AbstractSettings<YarnEnv>(), NpmApiExtension<YarnEnvironment, Yarn> {

    init {
        check(project == project.rootProject) {
            "Yarn plugin can be applied only to the root project, but was applied to ${project.path}"
        }
    }

    override val name: String
        get() = "yarn"

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    override val packageManager: Yarn by lazy {
        Yarn(
            objects = objects,
            execOps = execOps,
        )
    }

    /**
     * Retrieves the configured [YarnEnvironment] for this project.
     * After this property is accessed,
     * the other properties in [BaseYarnRootExtension] can no longer be modified.
     */
    override val environment: YarnEnvironment by lazy {
        requireConfigured().asYarnEnvironment
    }

    override val additionalInstallOutput: FileCollection =
        objects.fileCollection().from(
            nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
        )

    override val preInstallTasks: ListProperty<TaskProvider<*>> =
        objects.listProperty(TaskProvider::class.java)

    override val postInstallTasks: ListProperty<TaskProvider<*>> =
        objects.listProperty(TaskProvider::class.java)

    override val installationDirectory: DirectoryProperty =
        objects.directoryProperty()
            .fileValue(gradleHome.resolve("yarn"))

    /**
     * The base URL of the Ivy repository used to download the Yarn distribution.
     *
     * If set to `null`, the Ivy repository will not be added.
     */
    override val downloadBaseUrlProperty: org.gradle.api.provider.Property<String> =
        objects.property<String>()
            // use `value`, not `convention`, because this property can be nullable to not add repository
            .value("https://github.com/yarnpkg/yarn/releases/download")

    /**
     * The version of Yarn to download.
     */
    override val versionProperty: org.gradle.api.provider.Property<String> =
        objects.property<String>()
            .convention(KgpBuildConstants.DEFAULT_YARN_VERSION)

    override val commandProperty: org.gradle.api.provider.Property<String> =
        objects.property<String>()
            .convention("yarn")

    override val downloadProperty: org.gradle.api.provider.Property<Boolean> =
        objects.property<Boolean>()
            .convention(true)

    val lockFileNameProperty: org.gradle.api.provider.Property<String> =
        objects.property<String>()
            .convention(LockCopyTask.YARN_LOCK)

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [lockFileNameProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `lockFileNameProperty` instead. Scheduled for removal in 2.7.0.")
    var lockFileName: String by LegacyProperty(lockFileNameProperty)

    val lockFileDirectoryProperty: DirectoryProperty =
        objects.directoryProperty()
            .convention(
                objects.directoryProperty().fileValue(project.rootDir.resolve(LockCopyTask.KOTLIN_JS_STORE)),
            )

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [lockFileDirectoryProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `lockFileDirectoryProperty` instead. Scheduled for removal in 2.7.0.")
    var lockFileDirectory: File by LegacyProperty(
        lockFileDirectoryProperty.asFile,
        lockFileDirectoryProperty::set,
    )

    val ignoreScriptsProperty: org.gradle.api.provider.Property<Boolean> =
        objects.property<Boolean>()
            .convention(true)

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [ignoreScriptsProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `ignoreScriptsProperty` instead. Scheduled for removal in 2.7.0.")
    var ignoreScripts by LegacyProperty(ignoreScriptsProperty)

    val yarnLockMismatchReportProperty: org.gradle.api.provider.Property<YarnLockMismatchReport> =
        objects.property<YarnLockMismatchReport>()
            .convention(YarnLockMismatchReport.FAIL)

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [yarnLockMismatchReportProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `yarnLockMismatchReportProperty` instead. Scheduled for removal in 2.7.0.")
    var yarnLockMismatchReport: YarnLockMismatchReport by LegacyProperty(yarnLockMismatchReportProperty)

    val reportNewYarnLockProperty: org.gradle.api.provider.Property<Boolean> =
        objects.property<Boolean>().convention(false)

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [reportNewYarnLockProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `reportNewYarnLockProperty` instead. Scheduled for removal in 2.7.0.")
    var reportNewYarnLock: Boolean by LegacyProperty(reportNewYarnLockProperty)

    /**
     * If `true`, the `yarn.lock` file will automatically be updated.
     *
     * Use with caution.
     * Enabling this property will make the build less secure and non-reproducible.
     * Consider using a version control system to manage `yarn.lock` changes.
     */
    val yarnLockAutoReplaceProperty: org.gradle.api.provider.Property<Boolean> =
        objects.property<Boolean>().convention(false)

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [yarnLockAutoReplaceProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `yarnLockAutoReplaceProperty` instead. Scheduled for removal in 2.7.0.")
    var yarnLockAutoReplace: Boolean by LegacyProperty(yarnLockAutoReplaceProperty)

    val yarnSetupTaskProvider: TaskProvider<YarnSetupTask>
        get() = project.tasks
            .withType(YarnSetupTask::class.java)
            .named(nodeJsRoot.extensionName(YarnSetupTask.BASE_NAME))

    internal val platform: org.gradle.api.provider.Property<Platform> =
        objects.property(Platform::class.java)

    /**
     * This property has been migrated to use the Provider API.
     * Instead, use [resolutionsProperty].
     */
    @Deprecated("Updated to use the Provider API. Use `resolutionsProperty` instead. Scheduled for removal in 2.7.0.")
    // NOTE: Must use Property instead of LegacyProperty. The new `resolutionsProperty` must use `property` as a lazy convention.
    // If we used LegacyProperty then, if a user calls `resolutions.add(...)` then it will return a _copy_ of the list.
    // `add(...)` will mutate the _copy_ and the `resolutionsProperty` will not be updated.
    // Instead, use the deprecated Property and use the value of `resolutions` as its convention.
    var resolutions: MutableList<YarnResolution> by @Suppress("DEPRECATION") Property(mutableListOf())

    /**
     * The list of Yarn resolutions.
     *
     * See https://classic.yarnpkg.com/lang/en/docs/selective-version-resolutions/
     */
    val resolutionsProperty: ListProperty<YarnResolution> = objects.listProperty(YarnResolution::class.java)
        .convention(providers.provider { @Suppress("DEPRECATION") resolutions })

    /**
     * Add a resolution to the list of Yarn resolutions.
     *
     * See https://classic.yarnpkg.com/lang/en/docs/selective-version-resolutions/
     *
     * @see resolutionsProperty
     */
    fun resolution(path: String, configure: Action<YarnResolution>) {
        resolutionsProperty.add(
            YarnResolution(path)
                .apply { configure.execute(this) }
        )
    }

    /**
     * Add a resolution to the list of Yarn resolutions.
     *
     * See https://classic.yarnpkg.com/lang/en/docs/selective-version-resolutions/
     *
     * @see resolutionsProperty
     */
    fun resolution(path: String, version: String) {
        resolution(path) {
            it.include(version)
        }
    }

    internal val nodeJsEnvironment: org.gradle.api.provider.Property<NodeJsEnv> =
        objects.property(NodeJsEnv::class.java)

    override fun finalizeConfiguration(): YarnEnv {
        return yarnSpec.env.get()
    }

    val restoreYarnLockTaskProvider: TaskProvider<YarnLockCopyTask>
        get() = project.tasks.withType(YarnLockCopyTask::class.java)
            .named(nodeJsRoot.extensionName(YarnPlugin.RESTORE_YARN_LOCK_BASE_NAME))

    val storeYarnLockTaskProvider: TaskProvider<YarnLockStoreTask>
        get() = project.tasks.withType(YarnLockStoreTask::class.java)
            .named(nodeJsRoot.extensionName(YarnPlugin.STORE_YARN_LOCK_BASE_NAME))
}
