/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.cpp.CppConsumerPlugin
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.konan.target.*

plugins {
    kotlin("jvm")
    id("native-dependencies")
}

apply<CppConsumerPlugin>()

val cppRuntimeOnly by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

dependencies {
    api(project(":kotlin-stdlib"))
    api(project(":kotlin-native:Interop:Runtime"))
    api(project(":kotlin-native:libclangInterop"))
    implementation(project(":native:kotlin-native-utils"))

    testImplementation(kotlin("test-junit"))
    testImplementation(project(":compiler:util"))

    cppRuntimeOnly(project(":kotlin-native:libclangInterop"))
    cppRuntimeOnly(project(":kotlin-native:Interop:Runtime"))
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
    inputs.files(cppRuntimeOnly)
    dependsOn(nativeDependencies.llvmDependency)
    systemProperty("java.library.path", cppRuntimeOnly.elements.map { elements ->
        elements.joinToString(File.pathSeparator) { it.asFile.parentFile.absolutePath }
    })

    systemProperty("kotlin.native.llvm.libclang", "${nativeDependencies.llvmPath}/" + if (HostManager.hostIsMingw) {
        "bin/libclang.dll"
    } else {
        "lib/${System.mapLibraryName("clang")}"
    })

    systemProperty("kotlin.native.interop.indexer.temp", layout.buildDirectory.dir("testTemp").get().asFile)
}

