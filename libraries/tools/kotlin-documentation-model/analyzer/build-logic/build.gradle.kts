/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(libs.gradlePlugin.dokka)
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.shadow)
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
    implementation(libs.gradlePlugin.gradlePublish)

    // workaround for accessing version-catalog in convention plugins
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
