/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.bitcode.CompileToBitcodeExtension
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.properties.saveProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KOTLIN_NATIVE_STDLIB_NAME
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.konan.target.Architecture as TargetArchitecture

val kotlinVersion: String by rootProject.extra

plugins {
    id("base")
    id("compile-to-bitcode")
    id("runtime-testing")
}

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

googletest {
    revision = project.property("gtestRevision") as String
    refresh = project.hasProperty("refresh-gtest")
}

val targetList = enabledTargets(extensions.getByType<PlatformManager>())

bitcode {
    allTargets {
        module("main") {
            headersDirs.from("src/externalCallsChecker/common/cpp", "src/objcExport/cpp")
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

        // Headers from here get reused by Swift Export, so this module should not depend on anything in the runtime
        module("objcExport") {
            // There must not be any implementation files, only headers.
            sourceSets {}
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

            onlyIf { it.supportsMimallocAllocator() }
        }

        module("libbacktrace") {
            val elfSize = when (target.architecture) {
                TargetArchitecture.X64, TargetArchitecture.ARM64 -> 64
                TargetArchitecture.X86, TargetArchitecture.ARM32 -> 32
                else -> 32 // TODO(KT-66500): remove after the bootstrap
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

            onlyIf { it.supportsLibBacktrace() }
        }

        module("compiler_interface") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("launcher") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("debug") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("common_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/common"))
            headersDirs.from(files("src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/alloc/legacy/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("custom_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/custom"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/mimalloc/c/include", "src/alloc/common/cpp", "src/alloc/legacy/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }

            compilerArgs.add("-DKONAN_MI_MALLOC=1")
        }

        module("legacy_alloc") {
            srcRoot.set(layout.projectDirectory.dir("src/alloc/legacy"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("source_info_core_symbolication") {
            srcRoot.set(layout.projectDirectory.dir("src/source_info/core_symbolication"))
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }

            onlyIf { it.supportsCoreSymbolication() }
        }

        module("source_info_libbacktrace") {
            srcRoot.set(layout.projectDirectory.dir("src/source_info/libbacktrace"))
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/libbacktrace/c/include"))
            sourceSets {
                main {}
            }

            onlyIf { it.supportsLibBacktrace() }
        }

        module("objc") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("test_support") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                testFixtures {
                    inputFiles.include("**/*.cpp", "**/*.mm")
                }
            }
        }

        module("mm") {
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
            sourceSets {
                main {}
            }
        }

        module("noop_gc_custom") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/noop"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
            sourceSets {
                main {}
            }

            compilerArgs.add("-DCUSTOM_ALLOCATOR")
        }

        module("same_thread_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/stms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/legacy/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp", "src/alloc/custom/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("adaptive_gcScheduler") {
            srcRoot.set(layout.projectDirectory.dir("src/gcScheduler/adaptive"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
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
            headersDirs.from("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp")
            sourceSets {
                main {}
            }
        }

        module("noop_externalCallsChecker") {
            srcRoot.set(layout.projectDirectory.dir("src/externalCallsChecker/noop"))
            headersDirs.from("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp")
            sourceSets {
                main {}
            }
        }

        module("xctest_launcher") {
            headersDirs.from(files("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))

            sourceSets {
                main {}
            }
            onlyIf { it.family.isAppleFamily }
        }
    }
}

val objcExportApi by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

artifacts {
    // This should be a "public headers" directory and this configuration with artifacts should be defined by
    // CompileToBitcodePlugin itself.
    add(objcExportApi.name, layout.projectDirectory.dir("src/objcExport/cpp"))
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

targetList.forEach { target ->
    // TODO: replace with a more convenient user-facing task that can build for a specific target.
    //       like compileToBitcode with optional argument --target.
    tasks.register("${target}Runtime") {
        description = "Build all main runtime modules for $target"
        group = CompileToBitcodeExtension.BUILD_TASK_GROUP
        val dependencies = runtimeBitcode.incoming.artifactView {
            attributes {
                attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
            }
        }.files
        dependsOn(dependencies)
    }
}

val hostRuntime by tasks.registering {
    description = "Build all main runtime modules for host"
    group = CompileToBitcodeExtension.BUILD_TASK_GROUP
    dependsOn("${PlatformInfo.hostName}Runtime")
}

val hostRuntimeTests by tasks.registering {
    description = "Runs all runtime tests for host"
    group = CompileToBitcodeExtension.VERIFICATION_TASK_GROUP
    dependsOn("${PlatformInfo.hostName}RuntimeTests")
}

tasks.named("assemble") {
    dependsOn(targetList.map { "${it}Runtime" })
}

val hostAssemble by tasks.registering {
    dependsOn("${PlatformInfo.hostName}Runtime")
}

tasks.named("clean", Delete::class) {
    this.delete(layout.buildDirectory)
}

// region: Stdlib

val stdlibBuildTask by tasks.registering(KonanCompileTask::class) {
    group = BasePlugin.BUILD_GROUP
    description = "Build the Kotlin/Native standard library '$name'"

    this.compilerDistributionPath.set(kotlinNativeDist.absolutePath)
    dependsOn(":kotlin-native:distCompiler")

    this.konanTarget.set(HostManager.host)
    this.outputDirectory.set(
            layout.buildDirectory.dir("stdlib/${HostManager.hostName}/stdlib")
    )

    this.extraOpts.addAll(
            "-no-default-libs",
            "-no-endorsed-libs",
            "-nostdlib",
            "-Werror",
            "-Xexplicit-api=strict",
            "-Xexpect-actual-classes",
            "-module-name", KOTLIN_NATIVE_STDLIB_NAME,
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.native.internal.InternalForKotlinNative",
            "-language-version",
            "2.0",
            "-api-version",
            "2.0",
            "-Xdont-warn-on-error-suppression",
            "-Xstdlib-compilation",
            "-Xfragment-refines=nativeMain:common",
    )

    val common by sourceSets.creating {
        srcDir(project(":kotlin-stdlib").file("common/src/kotlin"))
        srcDir(project(":kotlin-stdlib").file("common/src/generated"))
        srcDir(project(":kotlin-stdlib").file("unsigned/src"))
        srcDir(project(":kotlin-stdlib").file("src"))
        srcDir(project(":kotlin-stdlib").file("native-wasm/src/"))
        srcDir(project(":kotlin-test").files("annotations-common/src/main/kotlin"))
        srcDir(project(":kotlin-test").files("common/src/main/kotlin"))
    }

    val nativeMain by sourceSets.creating {
        srcDir(project(":kotlin-native:Interop:Runtime").file("src/main/kotlin"))
        srcDir(project(":kotlin-native:Interop:Runtime").file("src/native/kotlin"))
        srcDir(project(":kotlin-native:Interop:JsRuntime").file("src/main/kotlin"))
        srcDir(project.file("src/main/kotlin"))
    }

    dependsOn(":prepare:build.version:writeStdlibVersion")
}

val stdlibTask = tasks.register<Copy>("nativeStdlib") {
    from(stdlibBuildTask.map { it.outputs.files })
    into(project.layout.buildDirectory.dir("nativeStdlib"))

    val allPossibleTargets = project.extensions.getByType<PlatformManager>().targetValues.map { it.name }
    val kotlinVersion = kotlinVersion
    eachFile {
        if (name == "manifest") {
            // Stdlib is a common library that doesn't depend on anything target-specific.
            // The current compiler can't create a library with manifest file that lists all targets.
            // So, add all targets to the manifest file.
            KFile(file.absolutePath).run {
                val props = loadProperties()
                props[KLIB_PROPERTY_NATIVE_TARGETS] = allPossibleTargets.joinToString(separator = " ")

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

val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets

cacheableTargetNames.forEach { targetName ->
    tasks.register("${targetName}StdlibCache", KonanCacheTask::class.java) {
        notCompatibleWithConfigurationCache("project used in execution time")
        target = targetName
        originalKlib.fileProvider(stdlibTask.map { it.destinationDir })
        klibUniqName = "stdlib"
        cacheRoot = project.layout.buildDirectory.dir("cache/$targetName").get().asFile.absolutePath

        dependsOn(":kotlin-native:${targetName}CrossDistRuntime")
        // stdlib cache links in runtime modules from the K/N distribution.
        inputs.dir("$kotlinNativeDist/konan/targets/$targetName/native")
    }
}

// endregion
