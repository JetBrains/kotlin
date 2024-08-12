/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import java.io.Serializable

/**
 * Package versions used by tasks
 */
// DO NOT MODIFY DIRECTLY! Use org.jetbrains.kotlin.generators.gradle.targets.js.MainKt
class NpmVersions : Serializable {
    val webpackDevServer = NpmPackageVersion("webpack-dev-server", "4.15.2")
    val webpack = NpmPackageVersion("webpack", "5.93.0")
    val webpackCli = NpmPackageVersion("webpack-cli", "5.1.4")
    val sourceMapLoader = NpmPackageVersion("source-map-loader", "5.0.0")
    val sourceMapSupport = NpmPackageVersion("source-map-support", "0.5.21")
    val cssLoader = NpmPackageVersion("css-loader", "7.1.2")
    val styleLoader = NpmPackageVersion("style-loader", "4.0.0")
    val sassLoader = NpmPackageVersion("sass-loader", "14.2.1")
    val sass = NpmPackageVersion("sass", "1.77.8")
    val toStringLoader = NpmPackageVersion("to-string-loader", "1.2.0")
    val miniCssExtractPlugin = NpmPackageVersion("mini-css-extract-plugin", "2.9.0")
    val mocha = NpmPackageVersion("mocha", "10.7.0")
    val karma = NpmPackageVersion("karma", "6.4.3")
    val karmaChromeLauncher = NpmPackageVersion("karma-chrome-launcher", "3.2.0")
    val karmaPhantomjsLauncher = NpmPackageVersion("karma-phantomjs-launcher", "1.0.4")
    val karmaFirefoxLauncher = NpmPackageVersion("karma-firefox-launcher", "2.1.3")
    val karmaOperaLauncher = NpmPackageVersion("karma-opera-launcher", "1.0.0")
    val karmaIeLauncher = NpmPackageVersion("karma-ie-launcher", "1.0.0")
    val karmaSafariLauncher = NpmPackageVersion("karma-safari-launcher", "1.0.0")
    val karmaMocha = NpmPackageVersion("karma-mocha", "2.0.1")
    val karmaWebpack = NpmPackageVersion("karma-webpack", "5.0.1")
    val karmaSourcemapLoader = NpmPackageVersion("karma-sourcemap-loader", "0.4.0")
    val typescript = NpmPackageVersion("typescript", "5.5.4")
    val kotlinWebHelpers = NpmPackageVersion("kotlin-web-helpers", "2.0.0")
}