/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.bitcode.CompileToBitcodeExtension
import org.jetbrains.kotlin.cpp.CppUsage
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
            sourceSets {
                main {
                    // TODO: Split out out `base` module and merge it together with `main` into `runtime.bc`
                    if (sanitizer == null) {
                        outputFile.set(layout.buildDirectory.file("bitcode/main/$target/runtime.bc"))
                    }
                }
                testFixtures {}
                test {}
            }
        }

        module("mimalloc") {
            sourceSets {
                main {
                    inputFiles.from(srcRoot.dir("c"))
                    inputFiles.include("**/*.c")
                    inputFiles.exclude("**/alloc-override*.c", "**/page-queue.c", "**/static.c", "**/bitmap.inc.c")
                    headersDirs.setFrom(srcRoot.dir("c/include"))
                }
            }

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

            onlyIf { target.supportsMimallocAllocator() }
        }

        module("libbacktrace") {
            val elfSize = when (target.architecture) {
                TargetArchitecture.X64, TargetArchitecture.ARM64 -> 64
                TargetArchitecture.X86, TargetArchitecture.ARM32,
                TargetArchitecture.MIPS32, TargetArchitecture.MIPSEL32,
                TargetArchitecture.WASM32 -> 32
            }
            val useMachO = target.family.isAppleFamily
            val useElf = target.family in listOf(Family.LINUX, Family.ANDROID)

            sourceSets {
                main {
                    inputFiles.from(srcRoot.dir("c"))
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
                    headersDirs.setFrom(srcRoot.dir("c/include"))
                }
            }

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

            onlyIf { target.supportsLibBacktrace() }
        }

        module("compiler_interface") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("launcher") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("debug") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("std_alloc") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("custom_alloc") {
            headersDirs.from(files("src/main/cpp", "src/mm/cpp", "src/gc/common/cpp", "src/gcScheduler/common/cpp"))
            sourceSets {
                main {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("opt_alloc") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }

            compilerArgs.add("-DKONAN_MI_MALLOC=1")
        }

        module("exceptionsSupport") {
            srcRoot.set(layout.projectDirectory.dir("src/exceptions_support"))
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("source_info_core_symbolication") {
            srcRoot.set(layout.projectDirectory.dir("src/source_info/core_symbolication"))
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }

            onlyIf { target.supportsCoreSymbolication() }
        }

        module("source_info_libbacktrace") {
            srcRoot.set(layout.projectDirectory.dir("src/source_info/libbacktrace"))
            headersDirs.from(files("src/main/cpp", "src/libbacktrace/c/include"))
            sourceSets {
                main {}
            }

            onlyIf { target.supportsLibBacktrace() }
        }

        module("profileRuntime") {
            srcRoot.set(layout.projectDirectory.dir("src/profile_runtime"))
            sourceSets {
                main {}
            }
        }

        module("objc") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("test_support") {
            headersDirs.from(files("src/main/cpp"))
            sourceSets {
                testFixtures {
                    inputFiles.include("**/*.cpp", "**/*.mm")
                }
            }
        }

        module("experimental_memory_manager") {
            srcRoot.set(layout.projectDirectory.dir("src/mm"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }
        }

        module("experimental_memory_manager_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/mm"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/main/cpp", "src/custom_alloc/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("common_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/common"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        module("noop_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/noop"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                testFixtures {}
            }
        }

        module("noop_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/noop"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp", "src/custom_alloc/cpp"))
            sourceSets {
                main {}
                testFixtures {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("same_thread_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/stms"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }
        }

        module("same_thread_ms_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/stms"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp", "src/custom_alloc/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("concurrent_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/cms"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }
        }

        module("concurrent_ms_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/cms"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp", "src/custom_alloc/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("common_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/common"))
            headersDirs.from(files("src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        module("manual_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/manual"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("adaptive_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/adaptive"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        module("aggressive_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/aggressive"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("custom_alloc_runtime_tests") {
            testedModules.addAll("custom_alloc")
            testSupportModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "concurrent_ms_gc", "objc")
        }

        testsGroup("experimentalMM_mimalloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "same_thread_ms_gc", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_std_alloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "same_thread_ms_gc", "std_alloc", "objc")
        }

        testsGroup("experimentalMM_custom_alloc_runtime_tests") {
            testedModules.addAll("experimental_memory_manager_custom", "same_thread_ms_gc_custom")
            testSupportModules.addAll("main", "common_gc", "common_gcScheduler", "manual_gcScheduler", "custom_alloc", "objc")
        }

        testsGroup("experimentalMM_cms_mimalloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "concurrent_ms_gc", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_cms_std_alloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "concurrent_ms_gc", "std_alloc", "objc")
        }

        testsGroup("experimentalMM_cms_custom_alloc_runtime_tests") {
            testedModules.addAll("experimental_memory_manager_custom", "concurrent_ms_gc_custom")
            testSupportModules.addAll("main", "common_gc", "common_gcScheduler", "manual_gcScheduler", "custom_alloc", "objc")
        }

        testsGroup("experimentalMM_noop_mimalloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "noop_gc", "mimalloc", "opt_alloc", "objc")
        }

        testsGroup("experimentalMM_noop_std_alloc_runtime_tests") {
            testedModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "manual_gcScheduler", "noop_gc", "std_alloc", "objc")
        }

        testsGroup("experimentalMM_noop_custom_alloc_runtime_tests") {
            testedModules.addAll("experimental_memory_manager_custom", "noop_gc_custom")
            testSupportModules.addAll("main", "common_gc", "common_gcScheduler", "manual_gcScheduler", "custom_alloc", "objc")
        }

        testsGroup("aggressive_gcScheduler_runtime_tests") {
            testedModules.addAll("aggressive_gcScheduler")
            testSupportModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "noop_gc", "std_alloc", "objc")
        }

        testsGroup("adaptive_gcScheduler_runtime_tests") {
            testedModules.addAll("adaptive_gcScheduler")
            testSupportModules.addAll("main", "experimental_memory_manager", "common_gc", "common_gcScheduler", "noop_gc", "std_alloc", "objc")
        }
    }
}

val runtimeBitcode by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LLVM_BITCODE))
    }
}

dependencies {
    runtimeBitcode(project(":kotlin-native:runtime"))
}

targetList.forEach { targetName ->
    // TODO: replace with a more convenient user-facing task that can build for a specific target.
    //       like compileToBitcode with optional argument --target.
    tasks.register("${targetName}Runtime") {
        description = "Build all main runtime modules for $targetName"
        group = CompileToBitcodeExtension.BUILD_TASK_GROUP
        val dependencies = runtimeBitcode.incoming.artifactView {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, project.platformManager.targetByName(targetName).withSanitizer())
            }
        }.files
        dependsOn(dependencies)
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

tasks.named("assemble") {
    dependsOn(targetList.map { "${it}Runtime" })
}

val hostAssemble by tasks.registering {
    dependsOn("${hostName}Runtime")
}

tasks.named("clean") {
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
                "-language-version",
                "1.9",
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
            dependsOn(":prepare:build.version:writeStdlibVersion")
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
        val runtimeFiles = runtimeBitcode.incoming.artifactView {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, project.platformManager.targetByName(targetName).withSanitizer())
            }
        }.files
        from(runtimeFiles) {
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
