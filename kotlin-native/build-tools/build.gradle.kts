/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    // We explicitly configure versions of plugins in settings.gradle.kts.
    // due to https://github.com/gradle/gradle/issues/1697.
    id("kotlin")
    groovy
    `java-gradle-plugin`
}

buildscript {
    dependencies {
        classpath("com.google.code.gson:gson:2.8.6")
    }
}

val rootProperties = Properties().apply {
    rootDir.resolve("../gradle.properties").reader().use(::load)
}

val kotlinVersion: String by rootProperties
val kotlinCompilerRepo: String by rootProperties
val buildKotlinVersion: String by rootProperties
val buildKotlinCompilerRepo: String by rootProperties
val konanVersion: String by rootProperties
val slackApiVersion: String by rootProperties
val ktorVersion: String by rootProperties
val shadowVersion: String by rootProperties
val metadataVersion: String by rootProperties

group = "org.jetbrains.kotlin"
version = konanVersion

repositories {
    maven(kotlinCompilerRepo)
    maven(buildKotlinCompilerRepo)
    maven("https://cache-redirector.jetbrains.com/maven-central")
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    maven("https://cache-redirector.jetbrains.com/jcenter")
    jcenter()
}

dependencies {
    compileOnly(gradleApi())

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    api("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")

    // Located in <repo root>/shared and always provided by the composite build.
    api("org.jetbrains.kotlin:kotlin-native-shared:$konanVersion")
    implementation("com.github.jengelman.gradle.plugins:shadow:$shadowVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("$projectDir/../tools/benchmarks/shared/src/main/kotlin/report")
}

gradlePlugin {
    plugins {
        create("benchmarkPlugin") {
            id = "benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.KotlinNativeBenchmarkingPlugin"
        }
        create("compileBenchmarking") {
            id = "compile-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.CompileBenchmarkingPlugin"
        }
        create("swiftBenchmarking") {
            id = "swift-benchmarking"
            implementationClass = "org.jetbrains.kotlin.benchmark.SwiftBenchmarkingPlugin"
        }
        create("compileToBitcode") {
            id = "compile-to-bitcode"
            implementationClass = "org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin"
        }
        create("runtimeTesting") {
            id = "runtime-testing"
            implementationClass = "org.jetbrains.kotlin.testing.native.RuntimeTestingPlugin"
        }
    }
}

val compileKotlin: KotlinCompile by tasks
val compileGroovy: GroovyCompile by tasks

// https://youtrack.jetbrains.com/issue/KT-37435
compileKotlin.apply {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += listOf("-Xno-optimized-callable-references", "-Xskip-prerelease-check")
}

// Add Kotlin classes to a classpath for the Groovy compiler
compileGroovy.apply {
    classpath += project.files(compileKotlin.destinationDir)
    dependsOn(compileKotlin)
}
