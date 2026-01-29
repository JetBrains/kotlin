/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("swift-benchmarking")
}

kotlin {
    macosArm64()
}

swiftBenchmark {
    // NOTE: these properties should be kept in sync with Package.swift
    applicationName = "swiftInterop"
    swiftToolsVersion = "5.8"
    packageDirectory = layout.buildDirectory.dir("swiftpkg/benchmark")
}