/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
}

dependencies {
    embedded(project(":compiler:cli-jklib")) { isTransitive = false }
    embedded(project(":compiler:ir.serialization.jklib")) { isTransitive = false }
}

runtimeJar {
    archiveFileName.set("jklib-compiler.jar")
    manifest {
        attributes(
            "Class-Path" to "kotlin-compiler.jar kotlin-stdlib.jar kotlin-reflect.jar",
            "Main-Class" to "org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler"
        )
    }
}
