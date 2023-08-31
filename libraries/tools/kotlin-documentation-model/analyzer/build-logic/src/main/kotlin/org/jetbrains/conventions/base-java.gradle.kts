/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

/**
 * Base configuration for Java projects.
 *
 * This convention plugin contains shared Java config for both the [KotlinJvmPlugin] convention plugin and
 * the Gradle Plugin subproject (which cannot have the `kotlin("jvm")` plugin applied).
 */

plugins {
    id("org.jetbrains.conventions.base")
    java
}

java {
    toolchain {
        languageVersion.set(dokkaBuild.mainJavaVersion)
    }
    withSourcesJar()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    maxParallelForks = if (System.getenv("GITHUB_ACTIONS") != null) {
        Runtime.getRuntime().availableProcessors()
    } else {
        (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    }

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(dokkaBuild.testJavaLauncherVersion)
    })
}

dependencies {
    testImplementation(platform(libs.junit.bom))
}
