/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.webpack

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.EXTRACT
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.IMPORT
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.INLINE
import javax.inject.Inject

/**
 * Configures CSS support for Webpack.
 *
 * This rule applies to `.css` files.
 *
 * The `.css` files will be processed with loaders.
 * The enabled loaders are controlled by the [mode].
 *
 * See [org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.cssSupport]
 */
@Suppress("LeakingThis")
abstract class KotlinWebpackCssRule @Inject constructor(name: String) : KotlinWebpackRule(name) {

    /**
     * Controls the loaders that will be applied to `.css` files.
     *
     * Must be one of:
     * - [KotlinWebpackCssMode.EXTRACT]
     * - [KotlinWebpackCssMode.INLINE]
     * - [KotlinWebpackCssMode.IMPORT]
     *
     * Default: [KotlinWebpackCssMode.INLINE]
     */
    @get:Input
    abstract val mode: Property<String>

    init {
        mode.convention(INLINE)
        test.convention("/\\.css\$/")
    }

    override fun validate(): Boolean {
        if (mode.get() !in arrayOf(EXTRACT, INLINE, IMPORT)) {
            error(
                """
                Possible values for cssSupport.mode:
                - EXTRACT
                - INLINE
                - IMPORT
                """.trimIndent()
            )
        }
        return true
    }

    override fun dependencies(versions: NpmVersions): Collection<RequiredKotlinJsDependency> {
        return mutableListOf<RequiredKotlinJsDependency>().apply {
            add(versions.cssLoader)
            when (mode.get()) {
                EXTRACT -> add(versions.miniCssExtractPlugin)
                INLINE -> add(versions.styleLoader)
                IMPORT -> add(versions.toStringLoader)
            }
        }
    }

    override fun loaders(): List<Loader> = when (mode.get()) {
        EXTRACT -> listOf(
            Loader(
                loader = "MiniCssExtractPlugin.loader",
                prerequisites = listOf(
                    "const MiniCssExtractPlugin = require('mini-css-extract-plugin');",
                    "config.plugins.push(new MiniCssExtractPlugin())"
                )
            ),
        )

        INLINE -> listOf(Loader(loader = "'style-loader'"))
        IMPORT -> listOf(Loader(loader = "'to-string-loader'"))
        else -> listOf()
    } + Loader(loader = "'css-loader'")
}

/**
 * Valid values for [KotlinWebpackCssRule.mode].
 */
object KotlinWebpackCssMode {
    /** Enable `MiniCssExtractPlugin.loader` and `css-loader` loaders. */
    const val EXTRACT = "extract"

    /** Enable `style-loader` and `css-loader` loaders. */
    const val INLINE = "inline"

    /** Enable `to-string-loader` and `css-loader` loaders. */
    const val IMPORT = "import"
}
