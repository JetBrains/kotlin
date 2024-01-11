/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.tools.solib
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.konan.target.*

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("kotlin.native.build-tools-conventions")
    id("native-interop-plugin")
    id("native")
    id("native-dependencies")
}

val libclangextProject = project(":kotlin-native:libclangext")
val libclangextTask = libclangextProject.path + ":build"
val libclangextDir = libclangextProject.layout.buildDirectory.get().asFile
val libclangextIsEnabled = libclangextProject.findProperty("isEnabled")!! as Boolean


val libclang =
    if (HostManager.hostIsMingw) {
        "lib/libclang.lib"
    } else {
        "lib/${System.mapLibraryName("clang")}"
    }

val cflags = mutableListOf( "-I${nativeDependencies.llvmPath}/include",
        "-I${project(":kotlin-native:libclangext").projectDir.absolutePath}/src/main/include",
                            *platformManager.hostPlatform.clangForJni.hostCompilerArgsForJni)

val ldflags = mutableListOf("${nativeDependencies.llvmPath}/$libclang", "-L${libclangextDir.absolutePath}", "-lclangext")

if (libclangextIsEnabled) {
    assert(HostManager.hostIsMac)
    // Let some symbols be undefined to avoid linking unnecessary parts.
    val unnecessarySymbols = setOf(
            "__ZN4llvm7remarks11parseFormatENS_9StringRefE",
            "__ZN4llvm7remarks22createRemarkSerializerENS0_6FormatENS0_14SerializerModeERNS_11raw_ostreamE",
            "__ZN4llvm7remarks14YAMLSerializerC1ERNS_11raw_ostreamENS0_14UseStringTableE",
            "__ZN4llvm3omp22getOpenMPDirectiveNameENS0_9DirectiveE",
            "__ZN4llvm7remarks14RemarkStreamer13matchesFilterENS_9StringRefE",
            "__ZN4llvm7remarks14RemarkStreamer9setFilterENS_9StringRefE",
            "__ZN4llvm7remarks14RemarkStreamerC1ENSt3__110unique_ptrINS0_16RemarkSerializerENS2_14default_deleteIS4_EEEENS_8OptionalINS_9StringRefEEE",
            "__ZN4llvm3omp19getOpenMPClauseNameENS0_6ClauseE",
            "__ZN4llvm3omp28getOpenMPContextTraitSetNameENS0_8TraitSetE",
            "__ZN4llvm3omp31isValidTraitSelectorForTraitSetENS0_13TraitSelectorENS0_8TraitSetERbS3_",
            "__ZN4llvm3omp31isValidTraitSelectorForTraitSetENS0_13TraitSelectorENS0_8TraitSetERbS3_",
            "__ZN4llvm3omp33getOpenMPContextTraitPropertyNameENS0_13TraitPropertyE",
            "__ZN4llvm3omp33getOpenMPContextTraitSelectorNameENS0_13TraitSelectorE",
            "__ZN4llvm3omp35getOpenMPContextTraitSetForPropertyENS0_13TraitPropertyE",
            "__ZN4llvm3omp33getOpenMPContextTraitPropertyKindENS0_8TraitSetENS_9StringRefE",
            "__ZN4llvm3omp10OMPContextC2EbNS_6TripleE",
            "__ZN4llvm3omp33getOpenMPContextTraitPropertyKindENS0_8TraitSetENS0_13TraitSelectorENS_9StringRefE",
            "__ZN4llvm3omp33getOpenMPContextTraitPropertyNameENS0_13TraitPropertyENS_9StringRefE",
            "__ZN4llvm15OpenMPIRBuilder25getOpenMPDefaultSimdAlignERKNS_6TripleERKNS_9StringMapIbNS_15MallocAllocatorEEE",
            "__ZN4llvm3ARM10getCPUAttrENS0_8ArchKindE",
            "__ZN4llvm3ARM10getSubArchENS0_8ArchKindE",
            "__ZN4llvm3ARM11getArchNameENS0_8ArchKindE",
            "__ZN4llvm3ARM12parseArchISAENS_9StringRefE",
            "__ZN4llvm3ARM12parseCPUArchENS_9StringRefE",
            "__ZN4llvm3ARM13convertV9toV8ENS0_8ArchKindE",
            "__ZN4llvm3ARM13getDefaultCPUENS_9StringRefE",
            "__ZN4llvm3ARM13getDefaultFPUENS_9StringRefENS0_8ArchKindE",
            "__ZN4llvm3ARM14getFPUFeaturesENS0_7FPUKindERNSt3__16vectorINS_9StringRefENS2_9allocatorIS4_EEEE",
            "__ZN4llvm3ARM16parseArchProfileENS_9StringRefE",
            "__ZN4llvm3ARM16parseArchVersionENS_9StringRefE",
            "__ZN4llvm3ARM20fillValidCPUArchListERNS_15SmallVectorImplINS_9StringRefEEE",
            "__ZN4llvm3ARM20getDefaultExtensionsENS_9StringRefENS0_8ArchKindE",
            "__ZN4llvm3ARM20getExtensionFeaturesEyRNSt3__16vectorINS_9StringRefENS1_9allocatorIS3_EEEE",
            "__ZN4llvm3ARM21parseBranchProtectionENS_9StringRefERNS0_22ParsedBranchProtectionERS1_",
            "__ZN4llvm3ARM9parseArchENS_9StringRefE",
            "__ZN4llvm3X8612parseArchX86ENS_9StringRefEb",
            "__ZN4llvm3X8612parseTuneCPUENS_9StringRefEb",
            "__ZN4llvm3X8613getKeyFeatureENS0_7CPUKindE",
            "__ZN4llvm3X8617getFeaturesForCPUENS_9StringRefERNS_15SmallVectorImplIS1_EEb",
            "__ZN4llvm3X8618getFeaturePriorityENS0_17ProcessorFeaturesE",
            "__ZN4llvm3X8620fillValidCPUArchListERNS_15SmallVectorImplINS_9StringRefEEEb",
            "__ZN4llvm3X8620fillValidTuneCPUListERNS_15SmallVectorImplINS_9StringRefEEEb",
            "__ZN4llvm3X8621updateImpliedFeaturesENS_9StringRefEbRNS_9StringMapIbNS_15MallocAllocatorEEE",
            "__ZN4llvm3X8622getCPUDispatchManglingENS_9StringRefE",
            "__ZN4llvm3X8630validateCPUSpecificCPUDispatchENS_9StringRefE",
            "__ZN4llvm4CSKY11getArchNameENS0_8ArchKindE",
            "__ZN4llvm4CSKY12parseCPUArchENS_9StringRefE",
            "__ZN4llvm5RISCV12parseTuneCPUENS_9StringRefEb",
            "__ZN4llvm5RISCV20fillValidCPUArchListERNS_15SmallVectorImplINS_9StringRefEEEb",
            "__ZN4llvm5RISCV24fillValidTuneCPUArchListERNS_15SmallVectorImplINS_9StringRefEEEb",
            "__ZN4llvm5RISCV8parseCPUENS_9StringRefEb",
            "__ZN4llvm6AMDGPU13parseArchR600ENS_9StringRefE",
            "__ZN4llvm6AMDGPU15getArchAttrR600ENS0_7GPUKindE",
            "__ZN4llvm6AMDGPU15getArchNameR600ENS0_7GPUKindE",
            "__ZN4llvm6AMDGPU15parseArchAMDGCNENS_9StringRefE",
            "__ZN4llvm6AMDGPU17getArchAttrAMDGCNENS0_7GPUKindE",
            "__ZN4llvm6AMDGPU17getArchNameAMDGCNENS0_7GPUKindE",
            "__ZN4llvm6AMDGPU20fillAMDGPUFeatureMapENS_9StringRefERKNS_6TripleERNS_9StringMapIbNS_15MallocAllocatorEEE",
            "__ZN4llvm6AMDGPU20getCanonicalArchNameERKNS_6TripleENS_9StringRefE",
            "__ZN4llvm6AMDGPU21fillValidArchListR600ERNS_15SmallVectorImplINS_9StringRefEEE",
            "__ZN4llvm6AMDGPU21insertWaveSizeFeatureENS_9StringRefERKNS_6TripleERNS_9StringMapIbNS_15MallocAllocatorEEERNSt3__112basic_stringIcNS9_11char_traitsIcEENS9_9allocatorIcEEEE",
            "__ZN4llvm6AMDGPU23fillValidArchListAMDGCNERNS_15SmallVectorImplINS_9StringRefEEE",
            "__ZN4llvm6Triple13getOSTypeNameENS0_6OSTypeE",
            "__ZN4llvm6TripleC1ERKNS_5TwineE",
            "__ZN4llvm6TripleC1ERKNS_5TwineES3_S3_S3_",
            "__ZN4llvm7AArch6417getArchExtFeatureENS_9StringRefE",
            "__ZN4llvm7AArch6418parseArchExtensionENS_9StringRefE",
            "__ZN4llvm7AArch6420fillValidCPUArchListERNS_15SmallVectorImplINS_9StringRefEEE",
            "__ZN4llvm7AArch6420getExtensionFeaturesEyRNSt3__16vectorINS_9StringRefENS1_9allocatorIS3_EEEE",
            "__ZN4llvm7AArch648ArchInfo13findBySubArchENS_9StringRefE",
            "__ZN4llvm7AArch648parseCpuENS_9StringRefE",
            "__ZN4llvm7AArch649parseArchENS_9StringRefE",
            "__ZN4llvm7remarks14RemarkStreamerC1ENSt3__110unique_ptrINS0_16RemarkSerializerENS2_14default_deleteIS4_EEEENS2_8optionalINS_9StringRefEEE",
            "__ZN4llvm9LoongArch14isValidCPUNameENS_9StringRefE",
            "__ZN4llvm9LoongArch16fillValidCPUListERNS_15SmallVectorImplINS_9StringRefEEE",
            "__ZNK4llvm6Triple11getArchNameEv",
            "__ZNK4llvm6Triple11isArch32BitEv",
            "__ZNK4llvm6Triple11isArch64BitEv",
            "__ZNK4llvm6Triple12getOSVersionEv",
            "__ZNK4llvm6Triple13getVendorNameEv",
            "__ZNK4llvm6Triple14isLittleEndianEv",
            "__ZNK4llvm6Triple16getMacOSXVersionERNS_12VersionTupleE",
            "__ZNK4llvm6Triple17isMacOSXVersionLTEjjj",
            "__ZNK4llvm6Triple18getEnvironmentNameEv",
            "__ZNK4llvm6Triple21getEnvironmentVersionEv",
            "__ZNK4llvm6Triple23getOSAndEnvironmentNameEv",
            "__ZNK4llvm6Triple9getOSNameEv",
    )
    ldflags.addAll(
            listOf("-Wl,--no-demangle", "-Wl,-search_paths_first", "-Wl,-headerpad_max_install_names", "-Wl,-U,_futimens") +
                    unnecessarySymbols.map { "-Wl,-U,$it" }
    )

    val llvmLibs = listOf(
            "clangAST", "clangASTMatchers", "clangAnalysis", "clangBasic", "clangDriver", "clangEdit",
            "clangFrontend", "clangFrontendTool", "clangLex", "clangParse", "clangSema",
            "clangRewrite", "clangRewriteFrontend", "clangStaticAnalyzerFrontend",
            "clangStaticAnalyzerCheckers", "clangStaticAnalyzerCore", "clangSerialization",
            "clangToolingCore",
            "clangTooling", "clangFormat", "LLVMTarget", "LLVMMC", "LLVMLinker", "LLVMTransformUtils",
            "LLVMBitWriter", "LLVMBitReader", "LLVMAnalysis", "LLVMProfileData", "LLVMCore",
            "LLVMSupport", "LLVMBinaryFormat", "LLVMDemangle"
    ).map { "${nativeDependencies.llvmPath}/lib/lib${it}.a" }

    ldflags.addAll(llvmLibs)
    ldflags.addAll(listOf("-lpthread", "-lz", "-lm", "-lcurses"))
}

val solib = when{
    HostManager.hostIsMingw -> "dll"
    HostManager.hostIsMac -> "dylib"
    else -> "so"
}
val lib = if (HostManager.hostIsMingw) "lib" else "a"


native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    val cxxflags = listOf("-std=c++11", *cflags.toTypedArray())
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags(*cflags.toTypedArray(),
                  "-c", "-o", ruleOut(), ruleInFirst())
        }
        (".cpp" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
            flags(*cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }

    }
    sourceSet {
        "main-c" {
            dir("prebuilt/nativeInteropStubs/c")
        }
        "main-cpp" {
            dir("src/nativeInteropStubs/cpp")
        }
    }
    val objSet = arrayOf(sourceSets["main-c"]!!.transform(".c" to ".$obj"),
                         sourceSets["main-cpp"]!!.transform(".cpp" to ".$obj"))

    target(solib("clangstubs"), *objSet) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags(
            "-shared",
            "-o", ruleOut(), *ruleInAll(),
            *ldflags.toTypedArray())
    }
}

tasks.named(solib("clangstubs")).configure {
    dependsOn(":kotlin-native:libclangext:${lib("clangext")}")
}

sourceSets {
    "main" {
        java {
            srcDirs("prebuilt/nativeInteropStubs/kotlin")
        }
        kotlin{
        }
    }
}

dependencies {
    api(project(":kotlin-stdlib"))
    api(project(":kotlin-native:Interop:Runtime"))

    testImplementation(kotlin("test-junit"))
    testImplementation(project(":compiler:util"))
}

val nativelibs = project.tasks.register<Copy>("nativelibs") {
    val clangstubsSolib = solib("clangstubs")
    dependsOn(clangstubsSolib)

    from(layout.buildDirectory.dir(clangstubsSolib))
    into(layout.buildDirectory.dir("nativelibs"))
}

kotlinNativeInterop {
    this.create("clang") {
        defFile("clang.def")
        compilerOpts(cflags)
        linkerOpts = ldflags
        genTask.dependsOn(libclangextTask)
        genTask.inputs.dir(libclangextDir)
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        optIn.addAll(
                listOf(
                        "kotlinx.cinterop.BetaInteropApi",
                        "kotlinx.cinterop.ExperimentalForeignApi",
                )
        )
        freeCompilerArgs.addAll(
                listOf(
                        "-Xskip-prerelease-check",
                        // staticCFunction uses kotlin.reflect.jvm.reflect on its lambda parameter.
                        "-Xlambdas=class",
                )
        )
    }
}

tasks.withType<Test>().configureEach {
    val projectsWithNativeLibs = listOf(
            project, // Current one.
            project(":kotlin-native:Interop:Runtime")
    )
    dependsOn(projectsWithNativeLibs.map { "${it.path}:nativelibs" })
    dependsOn(nativeDependencies.llvmDependency)
    systemProperty("java.library.path", projectsWithNativeLibs.joinToString(File.pathSeparator) {
        it.layout.buildDirectory.dir("nativelibs").get().asFile.absolutePath
    })

    systemProperty("kotlin.native.llvm.libclang", "${nativeDependencies.llvmPath}/" + if (HostManager.hostIsMingw) {
        "bin/libclang.dll"
    } else {
        "lib/${System.mapLibraryName("clang")}"
    })

    systemProperty("kotlin.native.interop.indexer.temp", layout.buildDirectory.dir("testTemp").get().asFile)
}

// Please note that list of headers should be fixed manually.
// See KT-46231 for details.
tasks.register("updatePrebuilt") {
    dependsOn("genClangInteropStubs")

    doLast {
        copy {
            from(layout.buildDirectory.dir("nativeInteropStubs/clang/kotlin")) {
                include("clang/clang.kt")
            }
            into("prebuilt/nativeInteropStubs/kotlin")
        }

        copy {
            from(layout.buildDirectory.dir("interopTemp")) {
                include("clangstubs.c")
            }
            into("prebuilt/nativeInteropStubs/c")
        }
    }
}
