/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment

@Suppress("DEPRECATION")
class YarnEnvironment internal constructor(
    val executable: String,
    val standalone: Boolean,
    val ignoreScripts: Boolean,
     private val yarnResolutions: List<YarnResolution>,
    internal val yarnResolutions2: NamedDomainObjectContainer<YarnResolutionSpec>?,
) : PackageManagerEnvironment {

    @Deprecated("internal util")
    constructor(
        executable: String,
        standalone: Boolean,
        ignoreScripts: Boolean,
        yarnResolutions: List<YarnResolution>,
    ) : this(
        executable,
        standalone,
        ignoreScripts,
        yarnResolutions,
        yarnResolutions2 = null
    )

    @Deprecated("internal util")
    operator fun component1() = executable

    @Deprecated("internal util")
    operator fun component2() = standalone

    @Deprecated("internal util")
    operator fun component3() = ignoreScripts
    @Deprecated("internal util")
    operator fun component4() = yarnResolutions

    @Deprecated("internal util")
    @Suppress("DEPRECATION")
    fun copy(
        executable: String = this.executable,
        standalone: Boolean = this.standalone,
        ignoreScripts: Boolean = this.ignoreScripts,
        yarnResolutions: List<YarnResolution> = this.yarnResolutions,
    ): YarnEnvironment =
        YarnEnvironment(
            executable,
            standalone,
            ignoreScripts,
            yarnResolutions,
        )

}

@Suppress("DEPRECATION")
internal val YarnEnv.asYarnEnvironment
    get() = YarnEnvironment(
        executable,
        standalone,
        ignoreScripts,
        yarnResolutions = emptyList(),
        yarnResolutions
    )
