/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import javax.inject.Inject

@Suppress("LeakingThis")
abstract class KotlinWebpackScssRule @Inject constructor(name: String) : KotlinWebpackCssRule(name) {
    init {
        test.convention("/\\.(scss|sass)\$/")
    }

    override fun dependencies(versions: NpmVersions): Collection<RequiredKotlinJsDependency> {
        return super.dependencies(versions) + versions.sass + versions.sassLoader
    }

    override fun loaders(): List<Loader> {
        return super.loaders() + Loader(loader = "'sass-loader'")
    }
}
