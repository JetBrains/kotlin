/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.testing.native.*
import org.jetbrains.kotlin.bitcode.CompileToBitcode

plugins {
    id("compile-to-bitcode")
    id("runtime-testing")
}

googletest {
    // The latest release GTest (1.10.0) doesn't properly register skipped tests in an XML-report.
    // Therefore we use a fixed commit form the master branch where this problem is already fixed.
    // https://github.com/google/googletest/commit/07f4869221012b16b7f9ee685d94856e1fc9f361
    revision = "07f4869221012b16b7f9ee685d94856e1fc9f361"
    refresh = project.hasProperty("refresh-gtest")
}

fun CompileToBitcode.includeRuntime() {
    headersDirs += files("../common/src/hash/headers", "src/main/cpp")
}

val hostName: String by project
val targetList: List<String> by project

bitcode {
    create("runtime", file("src/main")) {
        dependsOn(
            ":common:${target}Hash",
            "${target}StdAlloc",
            "${target}OptAlloc",
            "${target}Mimalloc",
            "${target}Launcher",
            "${target}Debug",
            "${target}Release",
            "${target}Strict",
            "${target}Relaxed",
            "${target}ProfileRuntime",
            "${target}Objc",
            "${target}ExceptionsSupport"
        )
        includeRuntime()
        linkerArgs.add(project.file("../common/build/$target/hash.bc").path)
    }

    create("mimalloc") {
        language = CompileToBitcode.Language.C
        includeFiles = listOf("**/*.c")
        excludeFiles += listOf("**/alloc-override*.c", "**/page-queue.c", "**/static.c")
        srcDirs = files("$srcRoot/c")
        compilerArgs.add("-DKONAN_MI_MALLOC=1")
        headersDirs = files("$srcRoot/c/include")

        onlyIf { targetSupportsMimallocAllocator(target) }
    }

    create("launcher") {
        includeRuntime()
    }

    create("debug") {
        includeRuntime()
    }

    create("std_alloc")
    create("opt_alloc")

    create("exceptionsSupport", file("src/exceptions_support")) {
        includeRuntime()
    }

    create("release") {
        includeRuntime()
    }

    create("strict") {
        includeRuntime()
    }

    create("relaxed") {
        includeRuntime()
    }

    create("profileRuntime", file("src/profile_runtime"))

    create("objc") {
        includeRuntime()
    }
}

val hostRuntime by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val clean by tasks.registering {
    doLast {
        delete(buildDir)
    }
}

val generateJsMath by tasks.registering {
    dependsOn(":distCompiler")
    doLast {
        val distDir: File by project
        val jsinteropScript = if (PlatformInfo.isWindows()) "jsinterop.bat" else "jsinterop"
        val jsinterop = "$distDir/bin/$jsinteropScript"
        val targetDir = "$buildDir/generated"

        project.exec {
            commandLine(
                    jsinterop,
                    "-pkg", "kotlinx.interop.wasm.math",
                    "-o", "$targetDir/math",
                    "-target", "wasm32"
            )
        }

        val generated = file("$targetDir/math-build/natives/js_stubs.js")
        val mathJs = file("src/main/js/math.js")
        mathJs.writeText(
            "// NOTE: THIS FILE IS AUTO-GENERATED!\n" +
            "// Run ':runtime:generateJsMath' to re-generate it.\n\n"
        )
        mathJs.appendText(generated.readText())
    }
}
