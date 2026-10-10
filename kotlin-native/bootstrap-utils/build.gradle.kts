/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = "org.jetbrains.kotlin.build"
version = "1"

kotlin {
    jvmToolchain(17)
}

val bootstrapVersion = providers.fileContents(layout.projectDirectory.file("../../gradle.properties")).asText.map { content ->
    Properties().apply { load(content.reader()) }.getProperty("bootstrap.kotlin.default.version")
        ?: error("Missing bootstrap.kotlin.default.version")
}

dependencies {
    val kotlinVersion = bootstrapVersion.get()
    compileOnly("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-util-io:$kotlinVersion")
}

sourceSets.main {
    kotlin.srcDir("../../native/utils/src")
    kotlin.srcDir("../../compiler/util-io/src")
    kotlin.include("org/jetbrains/kotlin/konan/target/HostManager.kt")
    kotlin.include("org/jetbrains/kotlin/konan/target/ClangArgs.kt")
    kotlin.include("org/jetbrains/kotlin/konan/target/KonanProperties.kt")
    kotlin.include("org/jetbrains/kotlin/konan/target/TargetManager.kt")
    kotlin.include("org/jetbrains/kotlin/io/PropertyFileUtils.kt")
}
