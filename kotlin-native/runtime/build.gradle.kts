/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.bitcode.CompileToBitcodeExtension
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.saveProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.konan.target.Architecture as TargetArchitecture

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
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

googletest {
    revision = project.property("gtestRevision") as String
    refresh = project.hasProperty("refresh-gtest")
}

val hostName: String by project
val targetList: List<String> by project

bitcode {
    allTargets {
        module("main") {
            // TODO: Split out out `base` module and merge it together with `main` into `runtime.bc`
            if (sanitizer == null) {
                outputFile.set(layout.buildDirectory.file("bitcode/main/$target/runtime.bc"))
            }
        }

        module("mimalloc") {
            val srcRoot = file("src/mimalloc")
            compiler.set("clang")
            compilerArgs.set(listOfNotNull(
                    "-std=gnu11",
                    if (sanitizer == SanitizerKind.THREAD) { "-O1" } else { "-O3" },
                    "-DKONAN_MI_MALLOC=1",
                    "-Wno-unknown-pragmas",
                    "-ftls-model=initial-exec",
                    "-Wno-unused-function",
                    "-Wno-error=atomic-alignment",
                    "-Wno-unused-parameter", /* for windows 32 */
                    "-DMI_TSAN=1".takeIf { sanitizer == SanitizerKind.THREAD },
            ))
            inputFiles.from("$srcRoot/c")
            inputFiles.include("**/*.c")
            inputFiles.exclude("**/alloc-override*.c", "**/page-queue.c", "**/static.c", "**/bitmap.inc.c")
            headersDirs.setFrom("$srcRoot/c/include")

            onlyIf { targetSupportsMimallocAllocator(target.name) }
        }

        module("libbacktrace") {
            val srcRoot = file("src/libbacktrace")
            val elfSize = when (target.architecture) {
                TargetArchitecture.X64, TargetArchitecture.ARM64 -> 64
                TargetArchitecture.X86, TargetArchitecture.ARM32,
                TargetArchitecture.MIPS32, TargetArchitecture.MIPSEL32,
                TargetArchitecture.WASM32 -> 32
            }
            val useMachO = target.family.isAppleFamily
            val useElf = target.family in listOf(Family.LINUX, Family.ANDROID)
            compiler.set("clang")
            compilerArgs.set(listOfNotNull(
                    "-std=gnu11",
                    "-funwind-tables",
                    "-W",
                    "-Wall",
                    "-Wwrite-strings",
                    "-Wstrict-prototypes",
                    "-Wmissing-prototypes",
                    "-Wold-style-definition",
                    "-Wmissing-format-attribute",
                    "-Wcast-qual",
                    "-O2",
                    "-DBACKTRACE_ELF_SIZE=$elfSize".takeIf { useElf },
                    "-Wno-atomic-alignment"
            ))
            inputFiles.from("$srcRoot/c")
            inputFiles.include(listOfNotNull(
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
            ))
            headersDirs.setFrom("$srcRoot/c/include")

            onlyIf { targetSupportsLibBacktrace(target.name) }
        }

        module("compiler_interface") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("launcher") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("debug") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("std_alloc") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("opt_alloc") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("exceptionsSupport", file("src/exceptions_support")) {
            headersDirs.from(files("src/main/cpp"))
        }

        module("source_info_core_symbolication", file("src/source_info/core_symbolication")) {
            headersDirs.from(files("src/main/cpp"))
            onlyIf { targetSupportsCoreSymbolication(target.name) }
        }
        module("source_info_libbacktrace", file("src/source_info/libbacktrace")) {
            headersDirs.from(files("src/main/cpp", "src/libbacktrace/c/include"))
            onlyIf { targetSupportsLibBacktrace(target.name) }
        }

        module("strict") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("relaxed") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("profileRuntime", file("src/profile_runtime"))

        module("objc") {
            headersDirs.from(files("src/main/cpp"))
        }

        module("test_support", outputGroup = "test") {
            headersDirs.from(files("src/main/cpp"), googletest.headersDirs)
            dependsOn("downloadGoogleTest")
        }

        module("legacy_memory_manager", file("src/legacymm")) {
            headersDirs.from(files("src/main/cpp"))
        }

        module("experimental_memory_manager", file("src/mm")) {
            headersDirs.from(files("src/gc/common/cpp", "src/main/cpp"))
        }

        module("common_gc", file("src/gc/common")) {
            headersDirs.from(files("src/mm/cpp", "src/main/cpp"))
        }

        module("noop_gc", file("src/gc/noop")) {
            headersDirs.from(files("src/gc/noop/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
        }

        module("same_thread_ms_gc", file("src/gc/stms")) {
            headersDirs.from(files("src/gc/stms/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
        }

        module("concurrent_ms_gc", file("src/gc/cms")) {
            headersDirs.from(files("src/gc/cms/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))

            onlyIf { targetSupportsThreads(target.name) }
        }

        testsGroup("std_alloc_runtime_tests") {
            testedModules.addAll("main", "legacy_memory_manager", "strict", "std_alloc", "objc")
        }

        testsGroup("mimalloc_runtime_tests") {
            testedModules.addAll("main", "legacy_memory_manager", "strict", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_mimalloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "same_thread_ms_gc", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_std_alloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "same_thread_ms_gc", "std_alloc", "objc")
        }

        testsGroup("experimentalMM_cms_mimalloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "concurrent_ms_gc", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_cms_std_alloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "concurrent_ms_gc", "std_alloc", "objc")
        }

        testsGroup("experimentalMM_noop_mimalloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "noop_gc", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_noop_std_alloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "noop_gc", "std_alloc", "objc")
        }
    }
}

val hostRuntime by tasks.registering {
    description = "Build all main runtime modules for host"
    group = CompileToBitcodeExtension.BUILD_TASK_GROUP
    dependsOn("${hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    description = "Runs all runtime tests for host"
    group = CompileToBitcodeExtension.VERIFICATION_TASK_GROUP
    dependsOn("${hostName}RuntimeTests")
}

val assemble by tasks.getting {
    dependsOn(targetList.map { "${it}Runtime" })
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
                "-XXLanguage:+RangeUntilOperator",
        )

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
            include("runtime.bc", "compiler_interface.bc")
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
