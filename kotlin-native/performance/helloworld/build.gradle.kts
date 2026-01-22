/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.getNativeProgramExtension
import org.jetbrains.kotlin.getCompileOnlyBenchmarksOpts
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("compile-benchmarking")
}

fun getCompileOnlyBenchmarksOpts(project: Project, defaultCompilerOpts: List<String>): List<String> {
    val dist = project.file(project.findProperty("kotlin.native.home") ?: "dist")
    val useCache = !project.hasProperty("disableCompilerCaches")
    val cacheOption = "-Xcache-directory=$dist/klib/cache/${HostManager.host.name}-gSTATIC-system"
            .takeIf { useCache && !PlatformInfo.isWindows() } // TODO: remove target condition when we have cache support for other targets.
    return (project.findProperty("nativeBuildType") as String?)?.let {
        if (it.equals("RELEASE", true))
            listOf("-opt")
        else if (it.equals("DEBUG", true))
            listOfNotNull("-g", cacheOption)
        else listOf()
    } ?: defaultCompilerOpts + listOfNotNull(cacheOption?.takeIf { !defaultCompilerOpts.contains("-opt") })
}

val dist = file(findProperty("kotlin.native.home") ?: "dist")
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
            command("$dist/bin/konanc$toolSuffix", "$projectDir/src/main/kotlin/main.kt", "-o",
                    "$buildDir/program$binarySuffix", *(buildOpts.toTypedArray()))
        }
    }
}
