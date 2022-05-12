/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.bitcode.CompileToBitcode
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.saveProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.konan.target.Architecture as TargetArchitecture
import org.jetbrains.kotlin.konan.file.File as KFile

// These properties are used by the 'konan' plugin, thus we set them before applying it.
val distDir: File by project
val konanHome: String by extra(distDir.absolutePath)
extra["org.jetbrains.kotlin.native.home"] = konanHome

plugins {
    id("compile-to-bitcode")
    id("runtime-testing")
    id("konan")
}

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17)
}

googletest {
    revision = project.property("gtestRevision") as String
    refresh = project.hasProperty("refresh-gtest")
}

fun CompileToBitcode.includeRuntime() {
    headersDirs += files("src/main/cpp")
}

val hostName: String by project
val targetList: List<String> by project

bitcode {
    module("main") {
        includeRuntime()

        outputName = "runtime.bc"
    }

    module("mimalloc") {
        val srcRoot = file("src/mimalloc")
        language = CompileToBitcode.Language.C
        includeFiles = listOf("**/*.c")
        excludeFiles += listOf("**/alloc-override*.c", "**/page-queue.c", "**/static.c", "**/bitmap.inc.c")
        srcDirs = files("$srcRoot/c")
        compilerArgs.addAll(listOf("-DKONAN_MI_MALLOC=1", "-Wno-unknown-pragmas", "-ftls-model=initial-exec",
                "-Wno-unused-function", "-Wno-error=atomic-alignment",
                "-Wno-unused-parameter" /* for windows 32*/))
        extraSanitizerArgs[SanitizerKind.THREAD] = listOf("-DMI_TSAN=1")
        headersDirs = files("$srcRoot/c/include")

        onlyIf { targetSupportsMimallocAllocator(target) }
    }

    module("libbacktrace") {
        val srcRoot = file("src/libbacktrace")
        val targetInfo = HostManager().targetByName(target)
        language = CompileToBitcode.Language.C
        val useMachO = targetInfo.family.isAppleFamily
        val useElf = targetInfo.family in listOf(Family.LINUX, Family.ANDROID)
        includeFiles = listOfNotNull(
                "atomic.c",
                "backtrace.c",
                "dwarf.c",
                "elf.c".takeIf { useElf },
                "fileline.c",
                "macho.c".takeIf { useMachO },
                "mmap.c",
                "mmapio.c",
                "posix.c",
                "print.c",
                "simple.c",
                "sort.c",
                "state.c"
        )
        srcDirs = files("$srcRoot/c")
        val elfSize = when (targetInfo.architecture) {
            TargetArchitecture.X64, TargetArchitecture.ARM64 -> 64
            TargetArchitecture.X86, TargetArchitecture.ARM32,
            TargetArchitecture.MIPS32, TargetArchitecture.MIPSEL32,
            TargetArchitecture.WASM32 -> 32
        }
        compilerArgs.addAll(listOfNotNull(
                "-funwind-tables",
                "-W", "-Wall", "-Wwrite-strings", "-Wstrict-prototypes", "-Wmissing-prototypes",
                "-Wold-style-definition", "-Wmissing-format-attribute", "-Wcast-qual", "-O2",
                "-DBACKTRACE_ELF_SIZE=$elfSize".takeIf { useElf }, "-Wno-atomic-alignment"
        ))
        headersDirs = files("$srcRoot/c/include")

        onlyIf { targetSupportsLibBacktrace(target) }
    }


    module("launcher") {
        includeRuntime()
    }

    module("debug") {
        includeRuntime()
    }

    module("std_alloc") {
        includeRuntime()
    }

    module("opt_alloc") {
        includeRuntime()
    }

    module("exceptionsSupport", file("src/exceptions_support")) {
        includeRuntime()
    }

    module("source_info_core_symbolication", file("src/source_info/core_symbolication")) {
        includeRuntime()
        onlyIf { targetSupportsCoreSymbolication(target) }
    }
    module("source_info_libbacktrace", file("src/source_info/libbacktrace")) {
        includeRuntime()
        headersDirs += files("src/libbacktrace/c/include")
        onlyIf { targetSupportsLibBacktrace(target) }
    }

    module("strict") {
        includeRuntime()
    }

    module("relaxed") {
        includeRuntime()
    }

    module("profileRuntime", file("src/profile_runtime"))

    module("objc") {
        includeRuntime()
    }

    module("test_support", outputGroup = "test") {
        includeRuntime()
        dependsOn("downloadGoogleTest")
        headersDirs += googletest.headersDirs
    }

    module("legacy_memory_manager", file("src/legacymm")) {
        includeRuntime()
    }

    module("experimental_memory_manager", file("src/mm")) {
        headersDirs += files("src/gc/common/cpp")
        includeRuntime()
    }

    module("common_gc", file("src/gc/common")) {
        headersDirs += files("src/mm/cpp")
        includeRuntime()
    }

    module("noop_gc", file("src/gc/noop")) {
        headersDirs += files("src/gc/noop/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    }

    module("same_thread_ms_gc", file("src/gc/stms")) {
        headersDirs += files("src/gc/stms/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()
    }

    module("concurrent_ms_gc", file("src/gc/cms")) {
        headersDirs += files("src/gc/cms/cpp", "src/gc/common/cpp", "src/mm/cpp")
        includeRuntime()

        onlyIf { targetSupportsThreads(target) }
    }

    testsGroup("std_alloc_runtime_tests", listOf("main", "legacy_memory_manager", "strict", "std_alloc", "objc"))

    testsGroup("mimalloc_runtime_tests", listOf("main", "legacy_memory_manager", "strict", "mimalloc", "opt_alloc", "objc"))

    testsGroup("experimentalMM_mimalloc_runtime_tests", listOf("main", "experimental_memory_manager", "common_gc", "same_thread_ms_gc", "mimalloc", "opt_alloc", "objc"))

    testsGroup("experimentalMM_std_alloc_runtime_tests", listOf("main", "experimental_memory_manager", "common_gc", "same_thread_ms_gc", "std_alloc", "objc"))

    testsGroup("experimentalMM_cms_mimalloc_runtime_tests", listOf("main", "experimental_memory_manager", "common_gc", "concurrent_ms_gc", "mimalloc", "opt_alloc", "objc"))

    testsGroup("experimentalMM_cms_std_alloc_runtime_tests", listOf("main", "experimental_memory_manager", "common_gc", "concurrent_ms_gc", "std_alloc", "objc"))

    testsGroup("experimentalMM_noop_mimalloc_runtime_tests", listOf("main", "experimental_memory_manager", "common_gc", "noop_gc", "mimalloc", "opt_alloc", "objc"))

    testsGroup("experimentalMM_noop_std_alloc_runtime_tests", listOf("main", "experimental_memory_manager", "common_gc", "noop_gc", "std_alloc", "objc"))
}

val hostRuntime by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    dependsOn("${hostName}RuntimeTests")
}

val assemble by tasks.getting {
    dependsOn(targetList.forEach { "${it}Runtime" })
}

val hostAssemble by tasks.registering {
    dependsOn("${hostName}Runtime")
}

val clean by tasks.getting {
    doFirst {
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

// region: Stdlib

val commonStdlibSrcDirs = project(":kotlin-stdlib-common")
        .files(
                "src/kotlin",
                "src/generated",
                "../unsigned/src",
                "../src"
        ).files
val commonBuiltinsSrc = listOf(
        "Progressions.kt", "ProgressionIterators.kt", "Range.kt", "Ranges.kt", "internal/progressionUtil.kt")
        .map { "src/kotlin/$it" }
        .let {
            project(":core:builtins").files(it).files
        }

val interopRuntimeCommonSrcDir = project(":kotlin-native:Interop:Runtime").file("src/main/kotlin")
val interopSrcDirs = listOf(
        project(":kotlin-native:Interop:Runtime").file("src/native/kotlin"),
        project(":kotlin-native:Interop:JsRuntime").file("src/main/kotlin")
)

val testAnnotationCommonSrcDir = project(":kotlin-test:kotlin-test-annotations-common").files("src/main/kotlin").files
val testCommonSrcDir = project(":kotlin-test:kotlin-test-common").files("src/main/kotlin").files

val stdLibSrcDirs =  interopSrcDirs + listOf(
        project.file("src/main/kotlin"),
        project(":kotlin-stdlib-common").file("../native-wasm/src/")
)

lateinit var stdlibBuildTask: TaskProvider<Task>

konanArtifacts {
    library("stdlib") {
        baseDir(project.buildDir.resolve("stdlib"))

        enableMultiplatform(true)
        noStdLib(true)
        noPack(true)
        noDefaultLibs(true)
        noEndorsedLibs(true)

        extraOpts(project.globalBuildArgs)
        extraOpts(
                "-Werror",
                "-module-name", "stdlib",
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlin.contracts.ExperimentalContracts",
                "-opt-in=kotlin.ExperimentalMultiplatform",
                "-opt-in=kotlin.native.internal.InternalForKotlinNative",
        )

        srcFiles(commonBuiltinsSrc)
        commonStdlibSrcDirs.forEach { commonSrcDir(it) }
        testAnnotationCommonSrcDir.forEach { commonSrcDir(it) }
        testCommonSrcDir.forEach { commonSrcDir(it) }
        commonSrcDir(interopRuntimeCommonSrcDir)
        stdLibSrcDirs.forEach { srcDir(it) }
    }

    stdlibBuildTask = project.findKonanBuildTask("stdlib", project.platformManager.hostPlatform.target).apply {
        configure {
            dependsOn(":kotlin-native:distCompiler")
        }
    }
}

targetList.forEach { targetName ->
    tasks.register("${targetName}Stdlib", Copy::class.java) {
        require(::stdlibBuildTask.isInitialized)
        dependsOn(stdlibBuildTask)
        dependsOn("${targetName}Runtime")

        destinationDir = project.buildDir.resolve("${targetName}Stdlib")

        from(project.buildDir.resolve("stdlib/${hostName}/stdlib"))
        from(project.buildDir.resolve("bitcode/main/$targetName")) {
            include("runtime.bc")
            into("default/targets/$targetName/native")
        }

        if (targetName != hostName) {
            doLast {
                // Change target in manifest file
                with(KFile(destinationDir.resolve("default/manifest").absolutePath)) {
                    val props = loadProperties()
                    props[KLIB_PROPERTY_NATIVE_TARGETS] = targetName
                    saveProperties(props)
                }
            }
        }
    }

    val cacheableTargetNames: List<String> by project

    if (targetName in cacheableTargetNames) {
        tasks.register("${targetName}StdlibCache", KonanCacheTask::class.java) {
            target = targetName
            originalKlib = project.buildDir.resolve("${targetName}Stdlib")
            klibUniqName = "stdlib"
            cacheRoot = project.buildDir.resolve("cache/$targetName").absolutePath

            dependsOn("${targetName}Stdlib")
            dependsOn(":kotlin-native:${targetName}CrossDistRuntime")
        }
    }
}

// endregion
