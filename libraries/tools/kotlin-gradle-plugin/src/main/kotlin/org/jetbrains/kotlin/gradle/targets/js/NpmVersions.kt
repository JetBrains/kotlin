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
    val dukat = NpmPackageVersion("dukat", "0.5.8-rc.4")
    val webpack4 = NpmPackageVersion("webpack", "4.46.0")
    val webpackCli3 = NpmPackageVersion("webpack-cli", "3.3.12")
    val webpackDevServer = NpmPackageVersion("webpack-dev-server", "4.0.0-beta.2")
    val webpackDevServer3 = NpmPackageVersion("webpack-dev-server", "3.11.2")
    val sourceMapLoader1 = NpmPackageVersion("source-map-loader", "1.1.3")
    val webpack = NpmPackageVersion("webpack", "5.35.0")
    val webpackCli = NpmPackageVersion("webpack-cli", "4.6.0")
    val webpackBundleAnalyzer = NpmPackageVersion("webpack-bundle-analyzer", "4.4.1")
    val sourceMapLoader = NpmPackageVersion("source-map-loader", "2.0.1")
    val sourceMapSupport = NpmPackageVersion("source-map-support", "0.5.19")
    val cssLoader = NpmPackageVersion("css-loader", "5.2.4")
    val styleLoader = NpmPackageVersion("style-loader", "2.0.0")
    val toStringLoader = NpmPackageVersion("to-string-loader", "1.1.6")
    val miniCssExtractPlugin = NpmPackageVersion("mini-css-extract-plugin", "1.5.0")
    val mocha = NpmPackageVersion("mocha", "8.3.2")
    val karma = NpmPackageVersion("karma", "6.3.2")
    val karmaChromeLauncher = NpmPackageVersion("karma-chrome-launcher", "3.1.0")
    val karmaPhantomjsLauncher = NpmPackageVersion("karma-phantomjs-launcher", "1.0.4")
    val karmaFirefoxLauncher = NpmPackageVersion("karma-firefox-launcher", "2.1.0")
    val karmaOperaLauncher = NpmPackageVersion("karma-opera-launcher", "1.0.0")
    val karmaIeLauncher = NpmPackageVersion("karma-ie-launcher", "1.0.0")
    val karmaSafariLauncher = NpmPackageVersion("karma-safari-launcher", "1.0.0")
    val karmaMocha = NpmPackageVersion("karma-mocha", "2.0.1")
    val karmaWebpack = NpmPackageVersion("karma-webpack", "5.0.0")
    val karmaCoverage = NpmPackageVersion("karma-coverage", "2.0.3")
    val karmaSourcemapLoader = NpmPackageVersion("karma-sourcemap-loader", "0.3.8")
    val formatUtil = NpmPackageVersion("format-util", "1.0.5")

    val kotlinJsTestRunner = KotlinGradleNpmPackage("test-js-runner")
}