/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

val npmPackages = listOf(
    NpmPackage("dukat", "0.5.8-rc.4"),
    NpmPackage("webpack"),
    NpmPackage("webpack", "4.46.0", "webpack4"),
    NpmPackage("webpack-cli"),
    NpmPackage("webpack-bundle-analyzer"),
    NpmPackage("webpack-dev-server"),
    NpmPackage("source-map-loader"),
    NpmPackage("source-map-loader", "1.1.3", "sourceMapLoader1"),
    NpmPackage("source-map-support"),
    NpmPackage("css-loader"),
    NpmPackage("style-loader"),
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
)

data class NpmPackage(
    val name: String,
    val version: String? = null,
    val displayName: String = name
)