/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

/**
 * Utility to run ynit tests for K1 and K2 (analysis API).
 */

plugins {
    id("org.jetbrains.conventions.base")
    id("org.jetbrains.conventions.base-java")
}

val descriptorsTestConfiguration: Configuration by configurations.creating {
    extendsFrom(configurations.testImplementation.get())
}
val symbolsTestConfiguration: Configuration by configurations.creating {
    extendsFrom(configurations.testImplementation.get())
}

val symbolsTest = tasks.register<Test>("symbolsTest") {
    useJUnitPlatform {
        excludeTags("onlyDescriptors", "onlyDescriptorsMPP")
    }
    classpath += symbolsTestConfiguration
}
// run symbols and descriptors tests
tasks.test {
    //enabled = false
    classpath += descriptorsTestConfiguration
    dependsOn(symbolsTest)
}

val descriptorsTest = tasks.register<Test>("descriptorsTest") {
    classpath += descriptorsTestConfiguration
}

tasks.check {
    dependsOn(symbolsTest)
}

