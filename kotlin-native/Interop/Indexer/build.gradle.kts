/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("jvm")
    id("native-dependencies")
}

dependencies {
    api(project(":kotlin-stdlib"))
    api(project(":kotlin-native:Interop:Runtime"))
    api(project(":kotlin-native:libclangInterop"))
    implementation(project(":native:kotlin-native-utils"))

    testImplementation(kotlin("test-junit"))
    testImplementation(project(":compiler:util"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        optIn.addAll(
                listOf(
                        "kotlinx.cinterop.BetaInteropApi",
                        "kotlinx.cinterop.ExperimentalForeignApi",
                )
        )
        freeCompilerArgs.addAll(
                listOf(
                        "-Xskip-prerelease-check",
                        // staticCFunction uses kotlin.reflect.jvm.reflect on its lambda parameter.
                        "-Xlambdas=class",
                )
        )
    }
}

tasks.withType<Test>().configureEach {
    val projectsWithNativeLibs = listOf(
            project(":kotlin-native:libclangInterop"),
            project(":kotlin-native:Interop:Runtime")
    )
    dependsOn(projectsWithNativeLibs.map { "${it.path}:nativelibs" })
    dependsOn(nativeDependencies.llvmDependency)
    systemProperty("java.library.path", projectsWithNativeLibs.joinToString(File.pathSeparator) {
        it.layout.buildDirectory.dir("nativelibs").get().asFile.absolutePath
    })

    systemProperty("kotlin.native.llvm.libclang", "${nativeDependencies.llvmPath}/" + if (HostManager.hostIsMingw) {
        "bin/libclang.dll"
    } else {
        "lib/${System.mapLibraryName("clang")}"
    })

    systemProperty("kotlin.native.interop.indexer.temp", layout.buildDirectory.dir("testTemp").get().asFile)
}

