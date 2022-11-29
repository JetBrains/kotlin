/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

val npmPackages = listOf(
    NpmPackage("webpack"),
    NpmPackage("webpack", "4.46.0", "webpack4"),
    NpmPackage("webpack-cli"),
    NpmPackage("webpack-cli", "3.3.12", "webpackCli3"),
    NpmPackage("webpack-bundle-analyzer"),
    NpmPackage("webpack-dev-server"),
    NpmPackage("webpack-dev-server", "3.11.2", "webpackDevServer3"),
    NpmPackage("source-map-loader"),
    NpmPackage("source-map-loader", "1.1.0", "sourceMapLoader1"),
    NpmPackage("source-map-support"),
    NpmPackage("css-loader"),
    NpmPackage("style-loader"),
    NpmPackage("sass-loader"),
    NpmPackage("sass"),
    NpmPackage("to-string-loader"),
    NpmPackage("mini-css-extract-plugin"),
    NpmPackage("mocha"),
    NpmPackage("karma"),
    NpmPackage("karma-chrome-launcher"),
    NpmPackage("karma-phantomjs-launcher"),
    NpmPackage("karma-firefox-launcher"),
    NpmPackage("karma-opera-launcher"),
    NpmPackage("karma-ie-launcher"),
    NpmPackage("karma-safari-launcher"),
    NpmPackage("karma-mocha"),
    NpmPackage("karma-webpack"),
    NpmPackage("karma-coverage"),
    NpmPackage("karma-sourcemap-loader"),
    NpmPackage("format-util"),
    NpmPackage("typescript"),
)

data class NpmPackage(
    val name: String,
    val version: String? = null,
    val displayName: String = name
)
