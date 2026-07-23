/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.NamedDomainObjectContainer

class NpmEnv internal constructor(
    val executable: String,
    val ignoreScripts: Boolean,
    val standalone: Boolean,
    val packageLockMismatchReport: LockFileMismatchReport,
    val reportNewPackageLock: Boolean,
    val packageLockAutoReplace: Boolean,
    private val deprecatedOverrides: List<@Suppress("DEPRECATION") NpmOverride> = emptyList(),
    internal val newOverrides: NamedDomainObjectContainer<NpmOverrideSpec>? = null,
) {
    @Deprecated("No longer supported. Scheduled for removal in Kotlin 2.7.")
    val overrides get() = deprecatedOverrides

    @Deprecated("Creating instances is not supported")
    constructor(
        executable: String,
        ignoreScripts: Boolean,
        standalone: Boolean,
        packageLockMismatchReport: LockFileMismatchReport,
        reportNewPackageLock: Boolean,
        packageLockAutoReplace: Boolean,
        overrides: List<@Suppress("DEPRECATION") NpmOverride>,
    ) : this(
        executable = executable,
        ignoreScripts = ignoreScripts,
        standalone = standalone,
        packageLockMismatchReport = packageLockMismatchReport,
        reportNewPackageLock = reportNewPackageLock,
        packageLockAutoReplace = packageLockAutoReplace,
        deprecatedOverrides = overrides,
    )

    @Deprecated("No longer a data class")
    operator fun component1() = executable

    @Deprecated("No longer a data class")
    operator fun component2() = ignoreScripts

    @Deprecated("No longer a data class")
    operator fun component3() = standalone

    @Deprecated("No longer a data class")
    operator fun component4() = packageLockMismatchReport

    @Deprecated("No longer a data class")
    operator fun component5() = reportNewPackageLock

    @Deprecated("No longer a data class")
    operator fun component6() = packageLockAutoReplace

    @Deprecated("No longer a data class")
    @Suppress("DEPRECATION")
    operator fun component7() = overrides

    @Deprecated("No longer supported. Scheduled for removal in Kotlin 2.7.")
    @Suppress("DEPRECATION")
    fun copy(
        executable: String = this.executable,
        ignoreScripts: Boolean = this.ignoreScripts,
        standalone: Boolean = this.standalone,
        packageLockMismatchReport: LockFileMismatchReport = this.packageLockMismatchReport,
        reportNewPackageLock: Boolean = this.reportNewPackageLock,
        packageLockAutoReplace: Boolean = this.packageLockAutoReplace,
        overrides: List<NpmOverride> = this.overrides,
    ): NpmEnv =
        NpmEnv(
            executable,
            ignoreScripts,
            standalone,
            packageLockMismatchReport,
            reportNewPackageLock,
            packageLockAutoReplace,
            overrides,
        )
}
