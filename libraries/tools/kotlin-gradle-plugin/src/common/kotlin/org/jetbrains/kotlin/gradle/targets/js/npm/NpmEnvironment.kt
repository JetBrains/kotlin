/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment

//data class NpmEnvironment(
//    val executable: String,
//    val ignoreScripts: Boolean,
//    val standalone: Boolean,
//    val overrides: List<NpmOverride>
//) : PackageManagerEnvironment


@Suppress("DEPRECATION")
class NpmEnvironment internal constructor(
    val executable: String,
    val standalone: Boolean,
    val ignoreScripts: Boolean,
    private val oldOverrides: List<NpmOverride>,
    internal val newOverrides: NamedDomainObjectContainer<NpmOverrideSpec>?,
) : PackageManagerEnvironment {

    @Deprecated("internal util")
    constructor(
        executable: String,
        standalone: Boolean,
        ignoreScripts: Boolean,
        overrides: List<NpmOverride>,
    ) : this(
        executable,
        standalone,
        ignoreScripts,
        overrides,
        newOverrides = null
    )

    @Deprecated("internal util")
    operator fun component1() = executable

    @Deprecated("internal util")
    operator fun component2() = standalone

    @Deprecated("internal util")
    operator fun component3() = ignoreScripts

    @Deprecated("internal util")
    operator fun component4() = oldOverrides

    @Deprecated("internal util")
    @Suppress("DEPRECATION")
    fun copy(
        executable: String = this.executable,
        standalone: Boolean = this.standalone,
        ignoreScripts: Boolean = this.ignoreScripts,
        overrides: List<NpmOverride> = this.oldOverrides,
    ): NpmEnvironment =
        NpmEnvironment(
            executable,
            standalone,
            ignoreScripts,
            overrides,
        )

}


@Suppress("DEPRECATION")
internal val NpmEnv.asNpmEnvironment
    get() = NpmEnvironment(
        executable,
        ignoreScripts,
        standalone,
        oldOverrides = emptyList(),
        newOverrides
    )
