/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.bitcode.CompileToBitcodeExtension
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCacheTask
import org.jetbrains.kotlin.gradle.plugin.konan.tasks.KonanCompileTask
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.library.KOTLIN_NATIVE_STDLIB_NAME
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution
import org.jetbrains.kotlin.nativeDistribution.registerNativeBootstrapDistribution
import org.jetbrains.kotlin.platformManager
import org.jetbrains.kotlin.konan.target.Architecture as TargetArchitecture

val kotlinVersion: String by rootProject.extra

plugins {
    id("base")
    id("compile-to-bitcode")
}

repositories {
    githubTag("google", "breakpad")
    githubCommit("google", "googletest")
}

val breakpad = configurations.dependencyScope("breakpad")
val breakpadClasspath = configurations.resolvable("breakpadClasspath") {
    extendsFrom(breakpad.get())
}
val googletest = configurations.dependencyScope("googletest")
val googletestClasspath = configurations.resolvable("googletestClasspath") {
    extendsFrom(googletest.get())
}
dependencies {
    breakpad("google:breakpad:2024.02.16@zip")
    // GTest 1.10.0 doesn't properly register skipped tests in an XML-report.
    // Therefore we use a fixed commit form the master branch where this problem is already fixed.
    // https://github.com/google/googletest/commit/07f4869221012b16b7f9ee685d94856e1fc9f361
    googletest("google:googletest:07f4869221012b16b7f9ee685d94856e1fc9f361@zip")
}

if (HostManager.host == KonanTarget.MACOS_ARM64) {
    project.configureJvmToolchain(JdkMajorVersion.JDK_17_0)
}

val breakpadLocationNoDependency = layout.buildDirectory.dir("breakpad")

val unpackBreakpad = tasks.register<Sync>("unpackBreakpad") {
    from(breakpadClasspath.map { zipTree(it.singleFile) })
    eachFile {
        relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
    }
    includeEmptyDirs = false
    into(breakpadLocationNoDependency)
}

val breakpadSources by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("sources-directory"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

artifacts {
    add(breakpadSources.name, unpackBreakpad)
}

val googletestLocationNoDependency = layout.buildDirectory.dir("googletest")

val unpackGoogletest = tasks.register<Sync>("unpackGoogletest") {
    from(googletestClasspath.map { zipTree(it.singleFile) })
    eachFile {
        relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
    }
    includeEmptyDirs = false
    into(googletestLocationNoDependency)
}

val targetList = enabledTargets(extensions.getByType<PlatformManager>())

// NOTE: the list of modules is duplicated in `RuntimeModule.kt`
bitcode {
    // Cannot use output of `unpackGoogletest` to support Gradle Configuration Cache working before `unpackGoogletest`
    // actually had a chance to run.
    googleTestHeadersNoDependency.from(
            googletestLocationNoDependency.map { it.dir("googletest/include") },
            googletestLocationNoDependency.map { it.dir("googlemock/include") }
    )
    googleTestHeadersDependency.set(unpackGoogletest.name)

    allTargets {
        val fixBrokenMacroExpansionInXcode15_3: List<String> = when (target) {
            KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64 -> hashMapOf(
                "TARGET_OS_OSX" to "1",
            )
            KonanTarget.IOS_ARM64 -> hashMapOf(
                "TARGET_OS_EMBEDDED" to "1",
                "TARGET_OS_IPHONE" to "1",
                "TARGET_OS_IOS" to "1",
            )
            KonanTarget.TVOS_ARM64 -> hashMapOf(
                "TARGET_OS_EMBEDDED" to "1",
                "TARGET_OS_IPHONE" to "1",
                "TARGET_OS_TV" to "1",
            )
            KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_DEVICE_ARM64 -> hashMapOf(
                "TARGET_OS_EMBEDDED" to "1",
                "TARGET_OS_IPHONE" to "1",
                "TARGET_OS_WATCH" to "1",
            )
            else -> emptyMap()
            }.map { "-D${it.key}=${it.value}" }

        val clangArgsSpecificForKonanSources: List<String> = run {
            val konanOptions = listOfNotNull(
                    target.architecture.name.takeIf { target != KonanTarget.WATCHOS_ARM64 },
                    "ARM32".takeIf { target == KonanTarget.WATCHOS_ARM64 },
                    target.family.name.takeIf { target.family != Family.MINGW },
                    "WINDOWS".takeIf { target.family == Family.MINGW },
                    "MACOSX".takeIf { target.family == Family.OSX },
                    "APPLE".takeIf { target.family.isAppleFamily },

                    "NO_64BIT_ATOMIC".takeUnless { target.supports64BitAtomics() },
                    "NO_UNALIGNED_ACCESS".takeUnless { target.supportsUnalignedAccess() },
                    "FORBID_BUILTIN_MUL_OVERFLOW".takeUnless { target.supports64BitMulOverflow() },

                    "OBJC_INTEROP".takeIf { target.supportsObjcInterop() },
                    "HAS_FOUNDATION_FRAMEWORK".takeIf { target.hasFoundationFramework() },
                    "HAS_UIKIT_FRAMEWORK".takeIf { target.hasUIKitFramework() },
                    "REPORT_BACKTRACE_TO_IOS_CRASH_LOG".takeIf { target.supportsIosCrashLog() },
                    "SUPPORTS_GRAND_CENTRAL_DISPATCH".takeIf { target.supportsGrandCentralDispatch },
                    "SUPPORTS_SIGNPOSTS".takeIf { target.supportsSignposts },
            ).map { "KONAN_$it=1" }
            val otherOptions = listOfNotNull(
                    "USE_ELF_SYMBOLS=1".takeIf { target.binaryFormat() == BinaryFormat.ELF },
                    "ELFSIZE=${target.pointerBits()}".takeIf { target.binaryFormat() == BinaryFormat.ELF },
                    "MACHSIZE=${target.pointerBits()}".takeIf { target.binaryFormat() == BinaryFormat.MACH_O },
                    "__ANDROID__".takeIf { target.family == Family.ANDROID },
                    "USE_PE_COFF_SYMBOLS=1".takeIf { target.binaryFormat() == BinaryFormat.PE_COFF },
                    "UNICODE".takeIf { target.family == Family.MINGW },
                    "USE_WINAPI_UNWIND=1".takeIf { target.supportsWinAPIUnwind() },
                    "USE_GCC_UNWIND=1".takeIf { target.supportsGccUnwind() }
            )
            (konanOptions + otherOptions).map { "-D$it" } + fixBrokenMacroExpansionInXcode15_3
        }

        defaultCompilerArgs.addAll(listOfNotNull(
                "-gdwarf-2".takeIf { kotlinBuildProperties.isNativeRuntimeDebugInfoEnabled },
                "-std=c++17",
                "-Werror",
                "-O2",
                "-fno-aligned-allocation", // TODO: Remove when all targets support aligned allocation in C++ runtime.
                "-Wall",
                "-Wextra",
                "-Wno-unused-parameter",  // False positives with polymorphic functions.
        ) + clangArgsSpecificForKonanSources)

        module("main") {
            headersDirs.from("src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/breakpad/cpp", "src/crashHandler/common/cpp", "src/utfcpp/cpp", "src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp",  "src/mm/cpp")
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
            testSupportModules.addAll("mm", "noop_externalCallsChecker", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        // Headers from here get reused by Swift Export, so this module should not depend on anything in the runtime
        module("objcExport") {
            // There must not be any implementation files, only headers.
            sourceSets {}
        }

        module("utfcpp") {
            // Header-only library
            sourceSets {}
        }

        if (!project.hasProperty("disableBreakpad")) {
            module("breakpad") {
                srcRoot.fileProvider(unpackBreakpad.map { it.destinationDir })
                val sources = listOf(
                        "client/mac/crash_generation/crash_generation_client.cc",
                        "client/mac/handler/breakpad_nlist_64.cc",
                        "client/mac/handler/dynamic_images.cc",
                        "client/mac/handler/exception_handler.cc",
                        "client/mac/handler/minidump_generator.cc",
                        "client/mac/handler/protected_memory_allocator.cc",
                        "client/minidump_file_writer.cc",
                        "common/mac/MachIPC.mm",
                        "common/mac/arch_utilities.cc",
                        "common/mac/file_id.cc",
                        "common/mac/macho_id.cc",
                        "common/mac/macho_utilities.cc",
                        "common/mac/macho_walker.cc",
                        "common/mac/string_utilities.cc",
                        "common/mac/bootstrap_compat.cc",
                        "common/convert_UTF.cc",
                        "common/md5.cc",
                        "common/string_conversion.cc",
                )
                sourceSets {
                    main {
                        inputFiles.from(srcRoot.dir("src"))
                        inputFiles.setIncludes(sources)
                        headersDirs.setFrom(project.layout.projectDirectory.dir("src/breakpad/cpp"))
                    }
                }
                // Make sure breakpad sources are downloaded when building the corresponding compilation database entry
                dependencies.add(unpackBreakpad)
                compilerArgs.set(listOf(
                        "-std=c++17",
                        "-DHAVE_MACH_O_NLIST_H",
                        "-DHAVE_CONFIG_H",
                ))

                onlyIf { it.family == Family.OSX }
            }
        } else {
            // Compiler expects breakpad.bc file. Let's give it an empty one.
            module("breakpad") {
                srcRoot.set(project.layout.projectDirectory.dir("src/breakpad_stubs"))
                sourceSets {
                    main {}
                }
            }
        }

        module("libbacktrace") {
            val elfSize = when (target.architecture) {
                TargetArchitecture.X64, TargetArchitecture.ARM64 -> 64
                TargetArchitecture.X86, TargetArchitecture.ARM32 -> 32
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
                    "-Werror",
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
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "custom_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
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
        }

        testsGroup("custom_alloc_test") {
            testedModules.addAll("custom_alloc")
            // TODO(KT-53776): Some tests depend on GC not being noop.
            testSupportModules.addAll("main", "noop_externalCallsChecker", "mm", "common_alloc", "common_gc", "concurrent_ms_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
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

        testsGroup("std_legacy_alloc_test") {
            testedModules.addAll("legacy_alloc")
            testSupportModules.addAll("main", "noop_externalCallsChecker", "mm", "common_alloc", "std_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
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

        module("googletest") {
            srcRoot.fileProvider(unpackGoogletest.map { it.destinationDir.resolve("googletest") })
            sourceSets {
                testFixtures {
                    inputFiles.from(srcRoot.dir("src"))
                    // That's how googletest/CMakeLists.txt builds gtest library.
                    inputFiles.include("gtest-all.cc")
                    // Cannot use output of `unpackGoogletest` to support Gradle Configuration Cache working before `unpackGoogletest`
                    // actually had a chance to run.
                    headersDirs.setFrom(
                            googletestLocationNoDependency.map { it.dir("googletest") },
                            googletestLocationNoDependency.map { it.dir("googletest/include") },
                    )
                    // `inputFiles` above is a `ConfigurableFileTree`. It gets resolved into a `FileCollection` during configuration phase
                    // in order to become an input for the `ClangFrontend` task. At configuration phase the result of `unpackGoogletest` is
                    // not yet available. Therefore, `inputFiles` expands into an empty list. To make it work correctly, we have to manually
                    // override the sources for the `ClangFrontend` task  here.
                    compileTask.configure {
                        inputFiles.setFrom(srcRoot.dir("src/gtest-all.cc"))
                    }
                }
            }
            compilerArgs.set(listOf("-std=c++17", "-O2"))
            // Make sure googletest sources are downloaded when building the corresponding compilation database entry
            dependencies.add(unpackGoogletest)
        }

        module("googlemock") {
            srcRoot.fileProvider(unpackGoogletest.map { it.destinationDir.resolve("googlemock") })
            sourceSets {
                testFixtures {
                    inputFiles.from(srcRoot.dir("src"))
                    // That's how googlemock/CMakeLists.txt builds gtest library.
                    inputFiles.include("gmock-all.cc")
                    // Cannot use output of `unpackGoogletest` to support Gradle Configuration Cache working before `unpackGoogletest`
                    // actually had a chance to run.
                    headersDirs.setFrom(
                            googletestLocationNoDependency.map { it.dir("googlemock") },
                            googletestLocationNoDependency.map { it.dir("googlemock/include") },
                            googletestLocationNoDependency.map { it.dir("googletest/include") },
                    )
                    // `inputFiles` above is a `ConfigurableFileTree`. It gets resolved into a `FileCollection` during configuration phase
                    // in order to become an input for the `ClangFrontend` task. At configuration phase the result of `unpackGoogletest` is
                    // not yet available. Therefore, `inputFiles` expands into an empty list. To make it work correctly, we have to manually
                    // override the sources for the `ClangFrontend` task  here.
                    compileTask.configure {
                        inputFiles.setFrom(srcRoot.dir("src/gmock-all.cc"))
                    }
                }
            }
            compilerArgs.set(listOf("-std=c++17", "-O2"))
            // Make sure googletest sources are downloaded when building the corresponding compilation database entry
            dependencies.add(unpackGoogletest)
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
                test {}
            }
        }

        testsGroup("mm_test") {
            testedModules.addAll("mm")
            testSupportModules.addAll("main", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
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
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "noop_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        module("noop_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/noop"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
            }
        }

        module("same_thread_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/stms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("stms_gc_test") {
            testedModules.addAll("same_thread_ms_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        testsGroup("stms_gc_custom_test") {
            testedModules.addAll("same_thread_ms_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        module("pmcs_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/pmcs"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                testFixtures {}
                test {}
            }
        }

        testsGroup("pmcs_gc_test") {
            testedModules.addAll("pmcs_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        testsGroup("pmcs_gc_custom_test") {
            testedModules.addAll("pmcs_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        module("concurrent_ms_gc") {
            srcRoot.set(layout.projectDirectory.dir("src/gc/cms"))
            headersDirs.from(files("src/alloc/common/cpp", "src/gcScheduler/common/cpp", "src/gc/common/cpp", "src/mm/cpp", "src/externalCallsChecker/common/cpp", "src/objcExport/cpp", "src/main/cpp"))
            sourceSets {
                main {}
                test {}
            }
        }

        testsGroup("cms_gc_test") {
            testedModules.addAll("concurrent_ms_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "common_alloc", "legacy_alloc", "std_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
        }

        testsGroup("cms_gc_custom_test") {
            testedModules.addAll("concurrent_ms_gc")
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "common_gcScheduler", "manual_gcScheduler", "objc", "noop_crashHandler")
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
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc", "manual_gcScheduler", "objc", "noop_crashHandler")
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
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc", "common_gcScheduler", "objc", "noop_crashHandler")
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
            testSupportModules.addAll("main", "mm", "noop_externalCallsChecker", "common_alloc", "custom_alloc", "common_gc", "noop_gc", "common_gcScheduler", "objc", "noop_crashHandler")
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

        if (!project.hasProperty("disableBreakpad")) {
            module("impl_crashHandler") {
                srcRoot.set(layout.projectDirectory.dir("src/crashHandler/impl"))
                // Cannot use output of `unpackBreakpad` to support Gradle Configuration Cache working before `unpackBreakpad`
                // actually had a chance to run.
                headersDirs.from("src/main/cpp", "src/breakpad/cpp", breakpadLocationNoDependency.get().dir("src"))
                sourceSets {
                    main {
                        // This task depends on breakpad headers being present.
                        compileTask.configure {
                            dependsOn(unpackBreakpad)
                        }
                    }
                }
                onlyIf { it.family == Family.OSX }
            }
        } else {
            module("impl_crashHandler") {
                srcRoot.set(layout.projectDirectory.dir("src/crashHandler/noop"))
                sourceSets {
                    main {}
                }
            }
        }

        module("noop_crashHandler") {
            srcRoot.set(layout.projectDirectory.dir("src/crashHandler/noop"))
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

val nativeBootstrapDistribution = registerNativeBootstrapDistribution()

val stdlibBuildTask by tasks.registering(KonanCompileTask::class) {
    group = BasePlugin.BUILD_GROUP
    description = "Build the Kotlin/Native standard library"

    this.compilerDistributionRoot.set(nativeBootstrapDistribution.map { it.root })

    this.outputDirectory.set(
            layout.buildDirectory.dir("stdlib/${HostManager.hostName}/stdlib")
    )

    this.extraOpts.addAll(listOfNotNull(
            "-no-default-libs",
            "-no-endorsed-libs",
            "-nostdlib",
            "-Werror".takeIf { !kotlinBuildProperties.disableWerror },
            "-Xallow-kotlin-package",
            "-Xexplicit-api=strict",
            "-Xexpect-actual-classes",
            "-Xklib-ir-inliner=intra-module",
            "-Xcontext-parameters",
            "-module-name", KOTLIN_NATIVE_STDLIB_NAME,
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.native.internal.InternalForKotlinNative",
            "-Xdont-warn-on-error-suppression",
            "-Xstdlib-compilation",
            "-Xklib-relative-path-base=${rootDir.canonicalPath}",
            "-Xklib-normalize-absolute-path",

            // See addReturnValueCheckerInfo() in libraries/stdlib/build.gradle.kts:
            "-Xreturn-value-checker=full",

            "-Xfragment-refines=nativeMain:nativeWasm,nativeMain:nativeWasmWasi,nativeMain:common,nativeWasmWasi:nativeWasm,nativeWasm:common,nativeWasm:commonNonJvm,commonNonJvm:common",
            "-Xmanifest-native-targets=${platformManager.targetValues.joinToString(separator = ",") { it.visibleName }}",

            // Between making a language feature stable and the next bootstrap, we need to keep providing the compiler argument.
            // But this produces a warning
            // "The argument ... is redundant for the current language version ..."
            // in the bootstrap test and fails because of -Werror.
            // To work around it, we suppress the warning.
            "-Xwarning-level=REDUNDANT_CLI_ARG:disabled",
    ))

    val common by sourceSets.creating {
        srcDir(project(":kotlin-stdlib").file("common/src/kotlin"))
        srcDir(project(":kotlin-stdlib").file("common/src/generated"))
        srcDir(project(":kotlin-stdlib").file("unsigned/src"))
        srcDir(project(":kotlin-stdlib").files("src").builtBy(":prepare:build.version:writeStdlibVersion"))
        srcDir(project(":kotlin-test").files("annotations-common/src/main/kotlin"))
        srcDir(project(":kotlin-test").files("common/src/main/kotlin"))
    }

    val commonNonJvm by sourceSets.creating {
        srcDir(project(":kotlin-stdlib").file("common-non-jvm/src"))
    }

    val nativeWasm by sourceSets.creating {
        srcDir(project(":kotlin-stdlib").file("native-wasm/src/"))
    }

    val nativeWasmWasi by sourceSets.creating {
        srcDir(project(":kotlin-stdlib").file("native-wasm/wasi/"))
    }

    val nativeMain by sourceSets.creating {
        srcDir(project(":kotlin-native:Interop:Runtime").file("src/main/kotlin"))
        srcDir(project(":kotlin-native:Interop:Runtime").file("src/native/kotlin"))
        srcDir(project.file("src/main/kotlin"))
    }
}

val nativeStdlib by tasks.registering(Sync::class) {
    from(stdlibBuildTask)
    into(project.layout.buildDirectory.dir("nativeStdlib"))
}

val cacheableTargetNames = platformManager.hostPlatform.cacheableTargets

cacheableTargetNames.forEach { targetName ->
    tasks.register("${targetName}StdlibCache", KonanCacheTask::class.java) {
        val dist = nativeDistribution

        // Requires Native distribution with stdlib klib and runtime modules for `targetName`.
        this.compilerDistributionRoot.set(dist.map { it.root })
        dependsOn(":kotlin-native:distCompiler")
        dependsOn(":kotlin-native:${targetName}CrossDistRuntime")
        inputs.dir(dist.map { it.runtime(targetName) }) // manually depend on runtime modules (stdlib cache links these modules in)

        this.klib.fileProvider(nativeStdlib.map { it.destinationDir })
        this.target.set(targetName)
        // This path is used in `:kotlin-native:${targetName}StdlibCache`
        this.outputDirectory.set(layout.buildDirectory.dir("cache/$targetName/$targetName-gSTATIC-system/$KOTLIN_NATIVE_STDLIB_NAME-cache"))
    }
}

// endregion
