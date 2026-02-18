/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.benchmarkingTargets
import org.jetbrains.kotlin.buildType
import org.jetbrains.kotlin.dryRun
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.kotlinNativeHome

plugins {
    id("benchmarking")
}

kotlin {
    benchmarkingTargets()
}

benchmark {
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
}

benchmark.konanRun.configure {
    reportFile.set(layout.buildDirectory.file("nativeBenchResults.unprocessed.json"))
    inputs.dir(kotlinNativeHome) // Make the entire used distribution an input
    environment.put("NATIVE_COMPILER", "${kotlinNativeHome}/bin/kotlinc-native${if (HostManager.hostIsMingw) ".bat" else ""}")
    inputs.property("compilerFlags", flags)
    environment.put("COMPILER_FLAGS", flags.joinToString(separator="\n"))
    val source = layout.projectDirectory.dir("testData").file("helloworld.kt")
    inputs.file(source)
    environment.put("SOURCE_FILE", source.asFile.absolutePath)
    outputs.file(outputBinary)
    environment.put("OUTPUT_BINARY", outputBinary.map { it.asFile.absolutePath })
}

val processBenchResults by tasks.registering {
    val unprocessedReportFile = benchmark.konanRun.map { it.reportFile.get() }
    inputs.file(unprocessedReportFile).withPathSensitivity(PathSensitivity.NONE) // just the contents of the report matters
    val reportFile = layout.buildDirectory.file("nativeBenchResults.json")
    outputs.file(reportFile)
    outputs.cacheIf { true }

    doFirst {
        val report = unprocessedReportFile.get().asFile.readText()
        // Let's not parse JSON and just do the simple substitution.
        reportFile.get().asFile.writeText(report.replace("EXECUTION_TIME", "COMPILE_TIME"))
    }
}

benchmark.getCodeSize.configure {
    isEnabled = !dryRun
    codeSizeBinary = outputBinary
    dependsOn(benchmark.konanRun) // make sure there's a dependency information attached to the input above
}

benchmark.konanJsonReport.configure {
    benchmarksReports.setFrom(benchmark.getCodeSize, processBenchResults)
    compilerFlags.set(flags)
}
