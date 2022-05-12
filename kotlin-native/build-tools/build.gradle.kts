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
    val rootBuildDirectory by extra(project.file("../.."))

    apply(from = rootBuildDirectory.resolve("kotlin-native/gradle/loadRootProperties.gradle"))
    dependencies {
        classpath(commonDependency("com.google.code.gson:gson"))
    }
}

val rootProperties = Properties().apply {
    project(":kotlin-native").projectDir.resolve("gradle.properties").reader().use(::load)
}

val kotlinVersion = project.bootstrapKotlinVersion
val konanVersion: String by rootProperties
val slackApiVersion: String by rootProperties
val ktorVersion: String by rootProperties
val shadowVersion: String by rootProperties
val metadataVersion: String by rootProperties

group = "org.jetbrains.kotlin"
version = konanVersion

repositories {
    maven("https://cache-redirector.jetbrains.com/maven-central")
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.ullink.slack:simpleslackapi:$slackApiVersion") {
        exclude(group = "com.google.code.gson", module = "gson") // Workaround for Gradle dependency resolution error
    }
    val versionPropertiesFile = project.rootProject.projectDir.resolve("gradle/versions.properties")
    val versionProperties = Properties()
    versionPropertiesFile.inputStream().use { propInput ->
        versionProperties.load(propInput)
    }
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion(versionProperties["versions.gson"] as String)
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    api(project(":native:kotlin-native-utils"))

    // Located in <repo root>/shared and always provided by the composite build.
//    api("org.jetbrains.kotlin:kotlin-native-shared:$kotlinVersion")
//    implementation("gradle.plugin.com.github.johnrengelman:shadow:$shadowVersion")
//
//    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")
}

sourceSets["main"].withConvention(KotlinSourceSet::class) {
    kotlin.srcDir("$projectDir/../tools/benchmarks/shared/src/main/kotlin/report")
}

val compileKotlin: KotlinCompile by tasks
val compileGroovy: GroovyCompile by tasks

compileKotlin.apply {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xskip-prerelease-check"
}

// Add Kotlin classes to a classpath for the Groovy compiler
compileGroovy.apply {
    classpath += project.files(compileKotlin.destinationDirectory)
    dependsOn(compileKotlin)
}
