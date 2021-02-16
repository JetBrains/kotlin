/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

val props = Properties().apply {
    project.file("../gradle.properties").inputStream().use { load(it) }
}

val kotlinVersion: String = props.getProperty("kotlinVersion")

dependencies {
    implementation(kotlin("gradle-plugin-api", kotlinVersion))
}

sourceSets["main"].withConvention(KotlinSourceSet::class) { kotlin.srcDirs("src") }

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        allWarningsAsErrors = true
        apiVersion = "1.3"
        freeCompilerArgs += "-Xskip-runtime-version-check"
    }
}
