/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency

@Suppress("LeakingThis")
abstract class KotlinWebpackScssRule : KotlinWebpackCssRule() {
    init {
        test.convention("/\\.(scss|sass)\$/")
    }

    override fun dependencies(versions: NpmVersions): Collection<RequiredKotlinJsDependency> {
        return super.dependencies(versions) + versions.sass + versions.sassLoader
    }

    override fun buildLoaders(): List<Loader> {
        return super.buildLoaders() + Loader(value = "'sass-loader'")
    }
}
