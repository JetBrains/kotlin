/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

/**
 * Package versions used by tasks
 */
// DO NOT MODIFY DIRECTLY! Use org.jetbrains.kotlin.generators.gradle.targets.js.MainKt
class NpmVersions {
    val dukat = NpmPackageVersion("dukat", "next")
    val webpack = NpmPackageVersion("webpack", "4.44.1")
    val webpackCli = NpmPackageVersion("webpack-cli", "3.3.12")
    val webpackBundleAnalyzer = NpmPackageVersion("webpack-bundle-analyzer", "3.8.0")
    val webpackDevServer = NpmPackageVersion("webpack-dev-server", "3.11.0")
    val sourceMapLoader = NpmPackageVersion("source-map-loader", "1.1.0")
    val sourceMapSupport = NpmPackageVersion("source-map-support", "0.5.19")
    val cssLoader = NpmPackageVersion("css-loader", "4.3.0")
    val styleLoader = NpmPackageVersion("style-loader", "1.2.1")
    val toStringLoader = NpmPackageVersion("to-string-loader", "1.1.6")
    val miniCssExtractPlugin = NpmPackageVersion("mini-css-extract-plugin", "0.11.1")
    val mocha = NpmPackageVersion("mocha", "8.1.3")
    val karma = NpmPackageVersion("karma", "5.2.2")
    val karmaChromeLauncher = NpmPackageVersion("karma-chrome-launcher", "3.1.0")
    val karmaPhantomjsLauncher = NpmPackageVersion("karma-phantomjs-launcher", "1.0.4")
    val karmaFirefoxLauncher = NpmPackageVersion("karma-firefox-launcher", "1.3.0")
    val karmaOperaLauncher = NpmPackageVersion("karma-opera-launcher", "1.0.0")
    val karmaIeLauncher = NpmPackageVersion("karma-ie-launcher", "1.0.0")
    val karmaSafariLauncher = NpmPackageVersion("karma-safari-launcher", "1.0.0")
    val karmaMocha = NpmPackageVersion("karma-mocha", "2.0.1")
    val karmaWebpack = NpmPackageVersion("karma-webpack", "4.0.2")
    val karmaCoverage = NpmPackageVersion("karma-coverage", "2.0.3")
    val karmaSourcemapLoader = NpmPackageVersion("karma-sourcemap-loader", "0.3.8")

    val kotlinJsTestRunner = KotlinGradleNpmPackage("test-js-runner")
}