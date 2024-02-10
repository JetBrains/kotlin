/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.tools.solib
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.Family.*
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsMac

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
            "__ZN4llvm3omp33getOpenMPContextTraitPropertyKindENS0_8TraitSetENS_9StringRefE"
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

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
                "-Xskip-prerelease-check",
                "-opt-in=kotlinx.cinterop.BetaInteropApi",
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                // staticCFunction uses kotlin.reflect.jvm.reflect on its lambda parameter.
                "-Xlambdas=class",
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
