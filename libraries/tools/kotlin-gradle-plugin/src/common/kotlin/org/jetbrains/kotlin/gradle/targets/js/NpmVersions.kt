/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import java.io.Serializable

/**
 * Package versions used by tasks
 */
// DO NOT MODIFY DIRECTLY! Use org.jetbrains.kotlin.generators.gradle.targets.js.MainKt
class NpmVersions : Serializable {
    val karma = NpmPackageVersion("karma", "github:Kotlin/karma#6.4.5")
    val webpack = NpmPackageVersion("webpack", "5.101.3")
    val webpackCli = NpmPackageVersion("webpack-cli", "6.0.1")
    val webpackDevServer = NpmPackageVersion("webpack-dev-server", "5.2.2")
    val sourceMapLoader = NpmPackageVersion("source-map-loader", "5.0.0")
    val sourceMapSupport = NpmPackageVersion("source-map-support", "0.5.21")
    val cssLoader = NpmPackageVersion("css-loader", "7.1.2")
    val styleLoader = NpmPackageVersion("style-loader", "4.0.0")
    val sassLoader = NpmPackageVersion("sass-loader", "16.0.5")
    val sass = NpmPackageVersion("sass", "1.92.0")
    val toStringLoader = NpmPackageVersion("to-string-loader", "1.2.0")
    val miniCssExtractPlugin = NpmPackageVersion("mini-css-extract-plugin", "2.9.4")
    val mocha = NpmPackageVersion("mocha", "11.7.2")
    val karmaChromeLauncher = NpmPackageVersion("karma-chrome-launcher", "3.2.0")

    @Deprecated(
        "Phantom JS is deprecated. Scheduled for removal in Kotlin 2.4.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("unused")
    val karmaPhantomjsLauncher = NpmPackageVersion("karma-phantomjs-launcher", "1.0.4")
    val karmaFirefoxLauncher = NpmPackageVersion("karma-firefox-launcher", "2.1.3")
    val karmaOperaLauncher = NpmPackageVersion("karma-opera-launcher", "1.0.0")
    val karmaIeLauncher = NpmPackageVersion("karma-ie-launcher", "1.0.0")
    val karmaSafariLauncher = NpmPackageVersion("karma-safari-launcher", "1.0.0")
    val karmaMocha = NpmPackageVersion("karma-mocha", "2.0.1")
    val karmaWebpack = NpmPackageVersion("karma-webpack", "5.0.1")
    val karmaSourcemapLoader = NpmPackageVersion("karma-sourcemap-loader", "0.4.0")
    val typescript = NpmPackageVersion("typescript", "5.9.2")
    val kotlinWebHelpers = NpmPackageVersion("kotlin-web-helpers", "3.0.0")

    val allDependencies = listOf(
        karma,
        webpack,
        webpackCli,
        webpackDevServer,
        sourceMapLoader,
        sourceMapSupport,
        cssLoader,
        styleLoader,
        sassLoader,
        sass,
        toStringLoader,
        miniCssExtractPlugin,
        mocha,
        karmaChromeLauncher,
        karmaFirefoxLauncher,
        karmaOperaLauncher,
        karmaIeLauncher,
        karmaSafariLauncher,
        karmaMocha,
        karmaWebpack,
        karmaSourcemapLoader,
        typescript,
        kotlinWebHelpers,
    )
}