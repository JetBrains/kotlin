/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("kotlin.native.build-tools-conventions")
    id("compile-to-bitcode")
}

bitcode {
    // These are only used in kotlin-native/backend.native/build.gradle where only the host target is needed.
    hostTarget {
        module("files") {
            headersDirs.from(layout.projectDirectory.dir("src/files/headers"))
            sourceSets {
                main {}
            }
        }
        module("env") {
            headersDirs.from(layout.projectDirectory.dir("src/env/headers"))
            sourceSets {
                main {}
            }
        }
    }
}

val hostName: String by project

tasks.register("build") {
    dependsOn("${hostName}Common")
}

tasks.register<Delete>("clean") {
    delete(buildDir)
}
