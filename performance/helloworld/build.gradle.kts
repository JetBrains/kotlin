/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.getNativeProgramExtension
import org.jetbrains.kotlin.getCompileOnlyBenchmarksOpts

plugins {
    id("compile-benchmarking")
}

val dist = file(findProperty("org.jetbrains.kotlin.native.home") ?: "dist")
val toolSuffix = if (System.getProperty("os.name").startsWith("Windows")) ".bat" else ""
val binarySuffix = getNativeProgramExtension()
val defaultCompilerOpts =  listOf("-g")
val buildOpts = getCompileOnlyBenchmarksOpts(project, defaultCompilerOpts)

compileBenchmark {
    applicationName = "HelloWorld"
    repeatNumber = 10
    compilerOpts = buildOpts
    buildSteps {
        step("runKonanc") {
            command("$dist/bin/konanc$toolSuffix", "$projectDir/src/main/kotlin/main.kt", "-o", "$buildDir/program$binarySuffix", *(buildOpts.toTypedArray()))
        }
    }
}
