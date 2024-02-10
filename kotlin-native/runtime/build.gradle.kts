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
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.konan.target.Architecture as TargetArchitecture

// These properties are used by the 'konan' plugin, thus we set them before applying it.
val distDir: File by project
val konanHome: String by extra(distDir.absolutePath)
extra["org.jetbrains.kotlin.native.home"] = konanHome

val kotlinVersion: String by rootProject.extra

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
            headersDirs.from("src/externalCallsChecker/common/cpp")
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

        testsGroup("main_test") {
            testedModules.addAll("main")
            // TODO(KT-53776): Some tests depend on allocator being legacy.
            testSupportModules.addAll("mm", "noop_externalCallsChecker", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
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
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("launcher") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("debug") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("common_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/common"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("common_alloc_test") {
            testedModules.addAll("common_alloc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "custom_alloc", "common_gc", "noop_gc_custom", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("std_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/std"))
            headersDirs.from(files("src/alloc/common/cpp", "src/alloc/legacy/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("custom_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/custom"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
                testFixtures {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        testsGroup("custom_alloc_test") {
            testedModules.addAll("custom_alloc")
            // TODO(KT-53776): Some tests depend on GC not being noop.
            testSupportModules.addAll("main", "noop_externalCallsChecker", "mm", "common_alloc", "common_gc", "concurrent_ms_gc_custom", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("mimalloc_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/mimalloc"))
            headersDirs.from(files("src/mimalloc/c/include", "src/alloc/common/cpp", "src/alloc/legacy/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }

            compilerArgs.add("-DKONAN_MI_MALLOC=1")
        }

        module("legacy_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/legacy"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
                testFixtures {}
            }
        }

        testsGroup("mimalloc_legacy_alloc_test") {
            testedModules.addAll("legacy_alloc")
            testSupportModules.addAll("main", "noop_externalCallsChecker", "mm", "common_alloc", "mimalloc_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "mimalloc")
        }

        testsGroup("std_legacy_alloc_test") {
            testedModules.addAll("legacy_alloc")
            testSupportModules.addAll("main", "noop_externalCallsChecker", "mm", "common_alloc", "std_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("exceptionsSupport") {
            srcRoot.set(layout.projectDirectory.dir("src/exceptions_support"))
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("source_info_core_symbolication") {
            srcRoot.set(layout.projectDirectory.dir("src/source_info/core_symbolication"))
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }

            onlyIf { target.supportsCoreSymbolication() }
        }

        module("source_info_libbacktrace") {
            srcRoot.set(layout.projectDirectory.dir("src/source_info/libbacktrace"))
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp", "src/libbacktrace/c/include"))
            sourceSets {
                main {}
            }

            onlyIf { target.supportsLibBacktrace() }
        }

        module("objc") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("test_support") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                testFixtures {
                    inputFiles.include("**/*.cpp", "**/*.mm")
                }
            }
        }

        module("mm") {
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }
        }

        testsGroup("mm_test") {
            testedModules.addAll("mm")
            testSupportModules.addAll("main", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc_custom", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("common_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/common"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("common_gc_test") {
            testedModules.addAll("common_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "noop_gc_custom", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("noop_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/noop"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
            sourceSets {
                main {}
            }
        }

        module("noop_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/noop"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
            sourceSets {
                main {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("same_thread_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/stms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("stms_gc_test") {
            testedModules.addAll("same_thread_ms_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("same_thread_ms_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/stms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
            sourceSets {
                main {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        testsGroup("stms_gc_custom_test") {
            testedModules.addAll("same_thread_ms_gc_custom")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("pmcs_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/pmcs"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("pmcs_gc_test") {
            testedModules.addAll("pmcs_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("pmcs_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/pmcs"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
            sourceSets {
                main {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        testsGroup("pmcs_gc_custom_test") {
            testedModules.addAll("pmcs_gc_custom")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("concurrent_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/cms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("cms_gc_test") {
            testedModules.addAll("concurrent_ms_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("concurrent_ms_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/cms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
            sourceSets {
                main {}
                test {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        testsGroup("cms_gc_custom_test") {
            testedModules.addAll("concurrent_ms_gc_custom")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc")
        }

        module("common_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/common"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("common_gcScheduler_test") {
            testedModules.addAll("common_gcScheduler")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc_custom", "manual_gcScheduler", "objc")
        }

        module("manual_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/manual"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("adaptive_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/adaptive"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("adaptive_gcScheduler_test") {
            testedModules.addAll("adaptive_gcScheduler")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc_custom", "common_gcScheduler", "objc")
        }

        module("aggressive_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/aggressive"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("aggressive_gcScheduler_test") {
            testedModules.addAll("aggressive_gcScheduler")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc_custom", "common_gcScheduler", "objc")
        }

        module("impl_externalCallsChecker") {
            srcRoot.set(layout.projectDirectory.dir("src/externalCallsChecker/impl"))
            headersDirs.from("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/main/cpp")
            sourceSets {
                main {}
            }
        }

        module("noop_externalCallsChecker") {
            srcRoot.set(layout.projectDirectory.dir("src/externalCallsChecker/noop"))
            headersDirs.from("src/externalCallsChecker/common/cpp", "src/main/cpp")
            sourceSets {
                main {}
            }
        }

        module("xctest_launcher") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/main/cpp"))

            sourceSets {
                main {}
            }
            onlyIf { target.family.isAppleFamily }
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
    doLast {
        delete(layout.buildDirectory)
    }
}

// region: Stdlib

val commonStdlibSrcDirs = project(":kotlin-stdlib")
        .files(
                "common/src/kotlin",
                "common/src/generated",
                "unsigned/src",
                "src"
        ).files

val interopRuntimeCommonSrcDir = project(":kotlin-native:Interop:Runtime").file("src/main/kotlin")
val interopSrcDirs = listOf(
        project(":kotlin-native:Interop:Runtime").file("src/native/kotlin"),
        project(":kotlin-native:Interop:JsRuntime").file("src/main/kotlin")
)

val testAnnotationCommonSrcDir = project(":kotlin-test").files("annotations-common/src/main/kotlin").files
val testCommonSrcDir = project(":kotlin-test").files("common/src/main/kotlin").files

val stdLibSrcDirs = interopSrcDirs + listOf(
        project.file("src/main/kotlin"),
        project(":kotlin-stdlib").file("native-wasm/src/")
)

lateinit var stdlibBuildTask: TaskProvider<Task>

konanArtifacts {
    library("stdlib") {
        baseDir(project.layout.buildDirectory.dir("stdlib").get().asFile)

        enableMultiplatform(true)
        noStdLib(true)
        noPack(true)
        noDefaultLibs(true)
        noEndorsedLibs(true)

        extraOpts(project.globalBuildArgs)
        extraOpts(
                "-Werror",
                "-Xexplicit-api=strict",
                "-Xexpect-actual-classes",
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

val stdlibTask = tasks.register<Copy>("nativeStdlib") {
    require(::stdlibBuildTask.isInitialized)

    from(stdlibBuildTask.map { it.outputs.files })
    into(project.layout.buildDirectory.dir("nativeStdlib"))

    eachFile {
        if (name == "manifest") {
            // Stdlib is a common library that doesn't depend on anything target-specific.
            // The current compiler can't create a library with manifest file that lists all supported targets.
            // So, add all supported targets to the manifest file.
            KFile(file.absolutePath).run {
                val props = loadProperties()
                props[KLIB_PROPERTY_NATIVE_TARGETS] = targetList.joinToString(separator = " ")

                // Check that we didn't get other than the requested version from cache, previous build or due to some other build issue
                val versionFromManifest = props[KLIB_PROPERTY_COMPILER_VERSION]
                check(versionFromManifest == kotlinVersion) {
                    "Manifest file ($this) processing: $versionFromManifest was found while $kotlinVersion was expected"
                }

                saveProperties(props)
            }
        }
    }
}

val cacheableTargetNames: List<String> by project

cacheableTargetNames.forEach { targetName ->
    tasks.register("${targetName}StdlibCache", KonanCacheTask::class.java) {
        target = targetName
        originalKlib.fileProvider(stdlibTask.map { it.destinationDir })
        klibUniqName = "stdlib"
        cacheRoot = project.layout.buildDirectory.dir("cache/$targetName").get().asFile.absolutePath

        dependsOn(":kotlin-native:${targetName}CrossDistRuntime")
        // stdlib cache links in runtime modules from the K/N distribution.
        inputs.dir("$distDir/konan/targets/$targetName/native")
    }
}

// endregion
