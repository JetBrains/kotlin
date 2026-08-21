/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.gradle.api.tasks.Exec

plugins {
    id("swift-benchmarking")
}

kotlin {
    macosArm64()
}

val weakRefFunctionAlignmentLog2 = providers.gradleProperty("weakRefFunctionAlignmentLog2")
    .map { it.toInt() }
val shouldDumpWeakRefLayout = providers.gradleProperty("dumpWeakRefLayout")
    .map { it.toBoolean() }
    .orElse(false)

swiftBenchmark {
    // NOTE: these properties should be kept in sync with Package.swift
    applicationName = "swiftInterop"
    swiftToolsVersion = "5.8"
    packageDirectory = layout.buildDirectory.dir("swiftpkg/benchmark")

    buildSwift.configure {
        weakRefFunctionAlignmentLog2.orNull?.let { alignment ->
            require(alignment in 4..6) {
                "weakRefFunctionAlignmentLog2 must be 4, 5, or 6"
            }
            options.addAll(
                "-Xswiftc", "-Xllvm",
                "-Xswiftc", "-align-all-functions=$alignment",
            )
        }
    }
}

val swiftInteropBinary = layout.buildDirectory.file("swiftInterop")
val weakRefLayoutDirectory = layout.buildDirectory.dir("weakref-layout")

val dumpWeakRefLayout by tasks.registering(Exec::class) {
    group = "benchmarking"
    description = "Collects post-run layout diagnostics for the Swift weak-reference benchmark."

    dependsOn(swiftBenchmark.buildSwift)
    inputs.file(swiftInteropBinary)
    outputs.dir(weakRefLayoutDirectory)

    doFirst {
        weakRefLayoutDirectory.get().asFile.mkdirs()
    }

    environment("BINARY", swiftInteropBinary.get().asFile.absolutePath)
    environment("OUT", weakRefLayoutDirectory.get().asFile.absolutePath)
    commandLine(
        "/bin/zsh", "-uc",
        """
        /usr/bin/shasum -a 256 "${'$'}BINARY" > "${'$'}OUT/sha256.txt"
        /usr/bin/stat -f "%z bytes" "${'$'}BINARY" > "${'$'}OUT/size.txt"
        /usr/bin/nm -nm "${'$'}BINARY" > "${'$'}OUT/nm.txt"
        /usr/bin/xcrun swift-demangle < "${'$'}OUT/nm.txt" > "${'$'}OUT/nm-demangled.txt" 2>&1 || true
        /usr/bin/otool -l "${'$'}BINARY" > "${'$'}OUT/load-commands.txt"
        /usr/bin/otool -Iv "${'$'}BINARY" > "${'$'}OUT/indirect-symbols.txt"
        /usr/bin/xcrun llvm-objdump --macho --arch=arm64 --disassemble --demangle \
            "${'$'}BINARY" > "${'$'}OUT/disassembly.txt" 2>&1 || true
        /bin/cp "${'$'}BINARY" "${'$'}OUT/swiftInterop"
        """.trimIndent(),
    )

    doLast {
        println(
            "##teamcity[publishArtifacts " +
                "'${weakRefLayoutDirectory.get().asFile.absolutePath} => weakref-layout']"
        )
    }
}

if (shouldDumpWeakRefLayout.get()) {
    swiftBenchmark.konanRun.configure {
        finalizedBy(dumpWeakRefLayout)
    }
}
