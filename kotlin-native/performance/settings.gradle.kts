/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

pluginManagement {
    includeBuild("../../repo/gradle-settings-conventions")

    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
        gradlePluginPortal()
    }
}

plugins {
    id("kotlin-bootstrap")
    id("jvm-toolchain-provisioning")
    id("kotlin-daemon-config")
    id("cache-redirector")
}

include(":benchmarksAnalyzer")
include(":benchmarksLauncher")
include(":benchmarksReports")
include(":ring")
include(":cinterop")
include(":helloworld")
include(":numerical")
include(":startup")
include(":logging")
if (System.getProperty("os.name") == "Mac OS X") {
    include(":objcinterop")
    include(":swiftinterop")
}
