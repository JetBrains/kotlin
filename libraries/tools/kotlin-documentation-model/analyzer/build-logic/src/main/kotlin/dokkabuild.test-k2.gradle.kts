/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Utility to run unit tests for K1 and K2 (analysis API).
 */

plugins {
    id("dokkabuild.base")
    id("dokkabuild.java")
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
    useJUnitPlatform {
        excludeTags("onlySymbols")
    }
    classpath += descriptorsTestConfiguration
    dependsOn(symbolsTest)
}

val descriptorsTest = tasks.register<Test>("descriptorsTest") {
    useJUnitPlatform {
        excludeTags("onlySymbols")
    }
    classpath += descriptorsTestConfiguration
}

tasks.check {
    dependsOn(symbolsTest)
}

