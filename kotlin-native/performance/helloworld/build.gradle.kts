/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmarkingTargets
import org.jetbrains.kotlin.buildType
import org.jetbrains.kotlin.compilerArgs
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.kotlinNativeHome

plugins {
    id("kotlinx-benchmarking")
}

kotlin {
    benchmarkingTargets()
}

kotlinxBenchmark {
    applicationName = "HelloWorld"
    prefixBenchmarksWithApplicationName = false
}

val outputBinary = layout.buildDirectory.file("program${if (HostManager.hostIsMingw) ".exe" else ".kexe"}")

val flags = buildList {
    when (project.buildType) {
        NativeBuildType.RELEASE -> add("-opt")
        NativeBuildType.DEBUG -> {
            add("-g")
            add("-Xauto-cache-from=${kotlinNativeHome}/klib/common")
            // Due to caches possibly being built on the first run, don't forget to have at least a single warmup round.
        }
    }
    addAll(project.compilerArgs)
}

benchmark {
    configurations.named("main").configure {
        val compilerFlags = flags.joinToString("!") {
            it.replace('=', '#')
        }
        param("nativeCompiler", "${kotlinNativeHome}/bin/kotlinc-native${if (HostManager.hostIsMingw) ".bat" else ""}")
        param("compilerFlags", compilerFlags)
        val source = layout.projectDirectory.dir("testData").file("helloworld.kt")
        param("sourceFile", source.asFile.absolutePath)
        param("outputBinary", outputBinary.map { it.asFile.absolutePath }.get())
    }
}

afterEvaluate {
    kotlinxBenchmark.runBenchmark.configure {
        inputs.dir(kotlinNativeHome) // Make the entire used distribution an input
        inputs.property("compilerFlags", flags)
        val source = layout.projectDirectory.dir("testData").file("helloworld.kt")
        inputs.file(source)
        outputs.file(outputBinary)
    }
}

kotlinxBenchmark.konanRun.configure {
    arguments.addAll("-m", "COMPILE_TIME")
}

kotlinxBenchmark.getCodeSize.configure {
    codeSizeBinary = outputBinary
    dependsOn(kotlinxBenchmark.runBenchmark) // make sure there's a dependency information attached to the input above
}

kotlinxBenchmark.konanJsonReport.configure {
    compilerFlags.set(flags)
}
