/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project

class NpmPackages {
    val webpack = NpmPackageVersion("webpack", "4.29.6")
    val webpackCli = NpmPackageVersion("webpack-cli", "3.3.0")
    val webpackBundleAnalyzer = NpmPackageVersion("webpack-bundle-analyzer", "3.3.2")
    val webpackDevServer = NpmPackageVersion("webpack-dev-server", "3.3.1")

    val sourceMapLoader = NpmPackageVersion("source-map-loader", "0.2.4")
    val sourceMapSupport = NpmPackageVersion("source-map-support", "0.5.12")

    val mocha = NpmPackageVersion("mocha", "6.1.2")

    val karma = NpmPackageVersion("karma", "4.0.1")
    val karmaTeamcityReporter = NpmPackageVersion("karma-teamcity-reporter", "1.1.0")

    val karmaChromeLauncher = NpmPackageVersion("karma-chrome-launcher", "2.2.0")
    val karmaPhantomJsLauncher = NpmPackageVersion("karma-phantomjs-launcher", "*")
    val karmaFirefoxLauncher = NpmPackageVersion("karma-firefox-launcher", "*")
    val karmaOperaLauncher = NpmPackageVersion("karma-opera-launcher", "*")
    val karmaIeLauncher = NpmPackageVersion("karma-ie-launcher", "*")
    val karmaSafariLauncher = NpmPackageVersion("karma-safari-launcher", "*")

    val karmaMocha = NpmPackageVersion("karma-mocha", "1.3.0")
    val karmaWebpack = NpmPackageVersion("karma-webpack", "*")

    val karmaSourceMapSupport = NpmPackageVersion("karma-source-map-support", "1.4.0")
}

data class NpmPackageVersion(val name: String, var version: String) {
    fun npm(project: Project) = NpmDependency(project, null, name, version)
}
