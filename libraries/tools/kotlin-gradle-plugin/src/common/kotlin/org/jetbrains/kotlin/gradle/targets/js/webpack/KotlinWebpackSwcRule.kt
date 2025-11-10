/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.swc.KotlinSwcConfig
import javax.inject.Inject

internal abstract class KotlinWebpackSwcRule
@Inject constructor(
    name: String,
) : KotlinWebpackRule(name) {
    init {
        test.convention("/\\.m?js\$/")
    }

    @get:Nested
    abstract val swcConfig: KotlinSwcConfig

    override fun dependencies(versions: NpmVersions): Collection<RequiredKotlinJsDependency> =
        super.dependencies(versions) + versions.swcCore + versions.swcLoader

    override fun loaders(): List<Loader> =
        listOf(
            Loader(loader = "'swc-loader'", options = swcConfig.toConfigMap())
        )
}

