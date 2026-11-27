/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.npm

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.DelicateKotlinGradlePluginApiKind.REQUIRES_KNOWLEDGE
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsVersion
import javax.inject.Inject

/**
 * Options of the `npmDependencyManagement { ... }` block shared by [JsNpmDependencyManagementDsl]
 * and [WasmJsNpmDependencyManagementDsl].
 *
 * Configures how the npm dependencies of the Kotlin/JS or Kotlin/Wasm JS projects of the build are installed.
 * The npm dependencies of all projects are installed together, with one npm install and one lockfile per build
 * for each of JS and Wasm JS. The two are configured independently.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     js {
 *         npmDependencyManagement {
 *             overrides.add("is-number", "7.0.0")
 *             lockFile.mismatchReport.set(NpmLockFileMismatchReport.WARNING)
 *         }
 *     }
 *     wasmJs {
 *         npmDependencyManagement {
 *             overrides.add("is-number", "7.0.0")
 *         }
 *     }
 * }
 * ```
 *
 * @since 2.5.20
 */
@ExperimentalNpmDependencyManagementDsl
abstract class NpmDependencyManagementDsl internal constructor(objects: ObjectFactory) {

    /**
     * Whether npm install runs with `--ignore-scripts`, so it does not run the install scripts of npm packages.
     *
     * Install scripts run arbitrary code from every package in the dependency tree,
     * so only disable this if a dependency needs its install scripts and you trust it.
     *
     * Default: `true`.
     *
     * @since 2.5.20
     */
    abstract val ignoreScripts: Property<Boolean>

    /**
     * The version of the Node.js toolchain used to install the npm dependencies.
     *
     * @since 2.5.20
     */
    @OptIn(ExperimentalNodeJsToolchainDsl::class)
    abstract val nodeJsVersionForDependencyResolution: Property<NodeJsVersion>

    /**
     * Versions of npm packages that replace the versions requested anywhere in the dependency tree,
     * for example to pick up a security fix in a transitive dependency.
     *
     * They are written to the [`overrides`](https://docs.npmjs.com/cli/configuring-npm/package-json#overrides)
     * field of the root `package.json`.
     *
     * @since 2.5.20
     */
    val overrides: OverridesDsl = objects.newInstance(OverridesDsl::class.java)

    /**
     * Configures [overrides].
     *
     * @since 2.5.20
     */
    fun overrides(configure: OverridesDsl.() -> Unit) {
        overrides.configure()
    }

    /**
     * [Action] based version of [overrides] above.
     *
     * @since 2.5.20
     */
    fun overrides(configure: Action<OverridesDsl>) {
        configure.execute(overrides)
    }

    /**
     * [overrides] of [NpmDependencyManagementDsl].
     *
     * @since 2.5.20
     */
    abstract class OverridesDsl @Inject internal constructor(objects: ObjectFactory) {

        /** Versions of the npm packages to override, by package name. */
        internal val versions: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

        /**
         * Installs [version] of the npm package [name] wherever it occurs in the dependency tree.
         *
         * @since 2.5.20
         */
        fun add(name: String, version: String) {
            versions.put(name, version)
        }

        /**
         * Lazy version of [add] above.
         *
         * @since 2.5.20
         */
        fun add(name: String, version: Provider<String>) {
            versions.put(name, version)
        }
    }

    /**
     * Configures the lockfile of the npm dependencies, which is stored in the repository,
     * so every build installs the same versions.
     *
     * @since 2.5.20
     */
    val lockFile: LockFileDsl = objects.newInstance(LockFileDsl::class.java)

    /**
     * [lockFile] of [NpmDependencyManagementDsl].
     *
     * @since 2.5.20
     */
    abstract class LockFileDsl internal constructor() {

        /**
         * The directory that stores the lockfile. Commit it to version control.
         *
         * Defaults:
         * - JS: `kotlin-js-store` in the settings directory.
         * - WasmJS: `kotlin-js-store/wasm` in the settings directory.
         *
         * @since 2.5.20
         */
        abstract val directory: DirectoryProperty

        /**
         * What happens when the lockfile resolved by npm install differs from the stored one.
         *
         * Default: [NpmLockFileMismatchReport.FAIL].
         *
         * @since 2.5.20
         */
        abstract val mismatchReport: Property<NpmLockFileMismatchReport>

        /**
         * Whether a lockfile that was not stored before counts as a mismatch, see [mismatchReport].
         *
         * Default: `false`.
         *
         * @since 2.5.20
         */
        abstract val reportNewLockFile: Property<Boolean>

        /**
         * Whether the stored lockfile is replaced by the resolved one on a mismatch.
         *
         * Default: `false`.
         *
         * @since 2.5.20
         */
        abstract val autoReplace: Property<Boolean>
    }

    /**
     * Whether KGP installs the npm dependencies of the projects of the build.
     *
     * Default: [SharedNpmDependenciesMode.SHARED_IN_ROOT_PROJECT].
     *
     * @since 2.5.20
     */
    @DelicateKotlinGradlePluginApi(REQUIRES_KNOWLEDGE)
    abstract val sharedNpmDependenciesMode: Property<SharedNpmDependenciesMode>
}
