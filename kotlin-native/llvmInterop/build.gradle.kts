import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.lib
import org.jetbrains.kotlin.tools.libname
import org.jetbrains.kotlin.tools.obj
import org.jetbrains.kotlin.tools.solib

plugins {
    kotlin("jvm")
    id("native-interop-plugin")
    id("native")
}

val defFileName = "llvm.def"
val usePrebuiltSources = false
val implementationDependencies = listOf(
        ":kotlin-native:llvmDebugInfoC",
        ":kotlin-native:libllvmext",
)
val commonCompilerArgs = buildList {
    addAll(listOf("-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wmissing-field-initializers"))
    addAll(listOf("-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wdelete-non-virtual-dtor"))
    addAll(listOf("-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS"))
}
val cCompilerArgs = listOf("-std=c99")
val cppCompilerArgs = listOf("-std=c++11")
val selfHeaders = emptyList<String>()
val systemIncludeDirs = listOf("${nativeDependencies.llvmPath}/include")
val linkerArgs = buildList {
    add("-fvisibility-inlines-hidden")
    addAll(listOf("-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wcast-qual", "-Wmissing-field-initializers"))
    addAll(listOf("-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wnon-virtual-dtor", "-Wdelete-non-virtual-dtor"))
    add("-std=c++17")
    addAll(listOf("-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS"))

    if (PlatformInfo.isMac()) {
        addAll(listOf("-Wl,-search_paths_first", "-Wl,-headerpad_max_install_names"))
        addAll(listOf("-lpthread", "-lz", "-lm", "-lcurses", "-Wl,-U,_futimens", "-Wl,-U,_LLVMDumpType"))
        add("-Wl,-exported_symbols_list,llvm.list")
        // $llvmDir/lib contains libc++.1.dylib too, and it seems to be preferred by the linker
        // over the sysroot-provided one.
        // As a result, libllvmstubs.dylib gets linked with $llvmDir/lib/libc++.1.dylib.
        // It has install_name = @rpath/libc++.1.dylib, which won't work for us, because
        // dynamic loader won't be able to find libc++ when loading libllvmstubs.
        // For some reason, this worked fine before macOS 12.3.
        //
        // To enforce linking with proper libc++, pass the default path explicitly:
        add("-L${nativeDependencies.hostPlatform.absoluteTargetSysRoot}/usr/lib")
    } else if (PlatformInfo.isLinux()) {
        add("-Wl,-z,noexecstack")
        addAll(listOf("-lrt", "-ldl", "-lpthread", "-lz", "-lm"))
    } else if (PlatformInfo.isWindows()) {
        addAll(listOf("-lpsapi", "-lshell32", "-lole32", "-luuid", "-ladvapi32"))
    }
}
val additionalLinkedStaticLibraries = buildList {
    val llvmLibs = if (PlatformInfo.isMac()) {
        // Produced by the following command:
        // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto objcarcopts arm aarch64 webassembly x86 mips
        "-lLLVMMipsDisassembler -lLLVMMipsAsmParser -lLLVMMipsCodeGen -lLLVMMipsDesc -lLLVMMipsInfo -lLLVMX86TargetMCA -lLLVMMCA -lLLVMX86Disassembler -lLLVMX86AsmParser -lLLVMX86CodeGen -lLLVMX86Desc -lLLVMX86Info -lLLVMWebAssemblyDisassembler -lLLVMWebAssemblyAsmParser -lLLVMWebAssemblyCodeGen -lLLVMWebAssemblyDesc -lLLVMWebAssemblyUtils -lLLVMWebAssemblyInfo -lLLVMAArch64Disassembler -lLLVMAArch64AsmParser -lLLVMAArch64CodeGen -lLLVMAArch64Desc -lLLVMAArch64Utils -lLLVMAArch64Info -lLLVMARMDisassembler -lLLVMARMAsmParser -lLLVMARMCodeGen -lLLVMCFGuard -lLLVMGlobalISel -lLLVMSelectionDAG -lLLVMAsmPrinter -lLLVMARMDesc -lLLVMMCDisassembler -lLLVMARMUtils -lLLVMARMInfo -lLLVMLTO -lLLVMRemoteCachingService -lLLVMRemoteNullService -lLLVMPasses -lLLVMCoroutines -lLLVMObjCARCOpts -lLLVMExtensions -lLLVMCodeGen -lLLVMCAS -lLLVMipo -lLLVMInstrumentation -lLLVMVectorize -lLLVMFrontendOpenMP -lLLVMScalarOpts -lLLVMInstCombine -lLLVMAggressiveInstCombine -lLLVMTarget -lLLVMLinker -lLLVMTransformUtils -lLLVMBitWriter -lLLVMAnalysis -lLLVMProfileData -lLLVMSymbolize -lLLVMDebugInfoPDB -lLLVMDebugInfoMSF -lLLVMDebugInfoDWARF -lLLVMObject -lLLVMTextAPI -lLLVMMCParser -lLLVMIRReader -lLLVMAsmParser -lLLVMMC -lLLVMDebugInfoCodeView -lLLVMBitReader -lLLVMCore -lLLVMRemarks -lLLVMBitstreamReader -lLLVMBinaryFormat -lLLVMSupport -lLLVMDemangle"
    } else if (PlatformInfo.isLinux()) {
        // Produced by the following command:
        // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto arm aarch64 webassembly x86 mips
        "-lLLVMMipsDisassembler -lLLVMMipsAsmParser -lLLVMMipsCodeGen -lLLVMMipsDesc -lLLVMMipsInfo -lLLVMX86TargetMCA -lLLVMMCA -lLLVMX86Disassembler -lLLVMX86AsmParser -lLLVMX86CodeGen -lLLVMX86Desc -lLLVMX86Info -lLLVMWebAssemblyDisassembler -lLLVMWebAssemblyAsmParser -lLLVMWebAssemblyCodeGen -lLLVMWebAssemblyDesc -lLLVMWebAssemblyUtils -lLLVMWebAssemblyInfo -lLLVMAArch64Disassembler -lLLVMAArch64AsmParser -lLLVMAArch64CodeGen -lLLVMAArch64Desc -lLLVMAArch64Utils -lLLVMAArch64Info -lLLVMARMDisassembler -lLLVMARMAsmParser -lLLVMARMCodeGen -lLLVMCFGuard -lLLVMGlobalISel -lLLVMSelectionDAG -lLLVMAsmPrinter -lLLVMARMDesc -lLLVMMCDisassembler -lLLVMARMUtils -lLLVMARMInfo -lLLVMLTO -lLLVMPasses -lLLVMIRPrinter -lLLVMCoroutines -lLLVMExtensions -lLLVMCodeGen -lLLVMObjCARCOpts -lLLVMipo -lLLVMInstrumentation -lLLVMVectorize -lLLVMFrontendOpenMP -lLLVMScalarOpts -lLLVMInstCombine -lLLVMAggressiveInstCombine -lLLVMTarget -lLLVMLinker -lLLVMTransformUtils -lLLVMBitWriter -lLLVMAnalysis -lLLVMProfileData -lLLVMSymbolize -lLLVMDebugInfoPDB -lLLVMDebugInfoMSF -lLLVMDebugInfoDWARF -lLLVMObject -lLLVMTextAPI -lLLVMMCParser -lLLVMIRReader -lLLVMAsmParser -lLLVMMC -lLLVMDebugInfoCodeView -lLLVMBitReader -lLLVMCore -lLLVMRemarks -lLLVMBitstreamReader -lLLVMBinaryFormat -lLLVMTargetParser -lLLVMSupport -lLLVMDemangle"
    } else if (PlatformInfo.isWindows()) {
        // Produced by the following command:
        // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto arm aarch64 webassembly x86
        "-lLLVMX86TargetMCA -lLLVMMCA -lLLVMX86Disassembler -lLLVMX86AsmParser -lLLVMX86CodeGen -lLLVMX86Desc -lLLVMX86Info -lLLVMWebAssemblyDisassembler -lLLVMWebAssemblyAsmParser -lLLVMWebAssemblyCodeGen -lLLVMWebAssemblyDesc -lLLVMWebAssemblyUtils -lLLVMWebAssemblyInfo -lLLVMAArch64Disassembler -lLLVMAArch64AsmParser -lLLVMAArch64CodeGen -lLLVMAArch64Desc -lLLVMAArch64Utils -lLLVMAArch64Info -lLLVMARMDisassembler -lLLVMARMAsmParser -lLLVMARMCodeGen -lLLVMCFGuard -lLLVMGlobalISel -lLLVMSelectionDAG -lLLVMAsmPrinter -lLLVMARMDesc -lLLVMMCDisassembler -lLLVMARMUtils -lLLVMARMInfo -lLLVMLTO -lLLVMPasses -lLLVMIRPrinter -lLLVMCoroutines -lLLVMExtensions -lLLVMCodeGen -lLLVMObjCARCOpts -lLLVMipo -lLLVMInstrumentation -lLLVMVectorize -lLLVMFrontendOpenMP -lLLVMScalarOpts -lLLVMInstCombine -lLLVMAggressiveInstCombine -lLLVMTarget -lLLVMLinker -lLLVMTransformUtils -lLLVMBitWriter -lLLVMAnalysis -lLLVMProfileData -lLLVMSymbolize -lLLVMDebugInfoPDB -lLLVMDebugInfoMSF -lLLVMDebugInfoDWARF -lLLVMObject -lLLVMTextAPI -lLLVMMCParser -lLLVMIRReader -lLLVMAsmParser -lLLVMMC -lLLVMDebugInfoCodeView -lLLVMBitReader -lLLVMCore -lLLVMRemarks -lLLVMBitstreamReader -lLLVMBinaryFormat -lLLVMTargetParser -lLLVMSupport -lLLVMDemangle"
    } else {
        error("Unsupported host platform")
    }
    llvmLibs.split(" ").mapTo(this) {
        "${nativeDependencies.llvmPath}/lib/${lib(it.removePrefix("-l"))}"
    }
}

val cppImplementation by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

val cppLink by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

dependencies {
    implementationDependencies.forEach {
        cppImplementation(project(it))
        cppLink(project(it))
    }
}

val includeDirs = project.files(*systemIncludeDirs.toTypedArray(), *selfHeaders.toTypedArray(), cppImplementation)

kotlinNativeInterop {
    create("main") {
        genTask.configure {
            defFile.set(project.file(defFileName))
            compilerOpts.set(cCompilerArgs + commonCompilerArgs)
            headersDirs.from(selfHeaders, cppImplementation)
            headersDirs.systemFrom(systemIncludeDirs)
        }
    }
}

val prebuiltRoot = layout.projectDirectory.dir("gen/main")
val generatedRoot = kotlinNativeInterop["main"].genTask.map { it.outputDirectory.get() }

val bindingsRoot = if (usePrebuiltSources) provider { prebuiltRoot } else generatedRoot

val stubsName = "${defFileName.removeSuffix(".def").split(".").reversed().joinToString(separator = "")}stubs"
val library = solib(stubsName)

val linkedStaticLibraries = project.files(cppLink.incoming.artifactView {
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.LINK_ARCHIVE))
    }
}.files, *additionalLinkedStaticLibraries.toTypedArray())

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            val cflags = cCompilerArgs + commonCompilerArgs + includeDirs.map { "-I${it.absolutePath}" } + nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni
            flags(*cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
        (".cpp" to ".$obj") {
            tool(*hostPlatform.clang.clangCXX("").toTypedArray())
            val cxxflags = cppCompilerArgs + commonCompilerArgs + includeDirs.map { "-I${it.absolutePath}" }
            flags(*cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main-c" {
            file(bindingsRoot.get().file("c/$stubsName.c").asFile.toRelativeString(layout.projectDirectory.asFile))
        }
        "main-cpp" {
            dir("src/main/cpp")
        }
    }
    val objSet = arrayOf(sourceSets["main-c"]!!.transform(".c" to ".$obj"),
            sourceSets["main-cpp"]!!.transform(".cpp" to ".$obj"))

    target(library, *objSet) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        val ldflags = buildList {
            addAll(linkedStaticLibraries.map { it.absolutePath })
            cppLink.incoming.artifactView {
                attributes {
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
                }
            }.files.flatMapTo(this) { listOf("-L${it.parentFile.absolutePath}", "-l${libname(it)}") }
            addAll(linkerArgs)
        }
        flags("-shared", "-o", ruleOut(), *ruleInAll(), *ldflags.toTypedArray())
    }
}

tasks.named(library).configure {
    inputs.files(linkedStaticLibraries).withPathSensitivity(PathSensitivity.NONE)
}
tasks.named(obj(stubsName)).configure {
    inputs.dir(bindingsRoot.map { it.dir("c") }).withPathSensitivity(PathSensitivity.RELATIVE) // if C file was generated, need to set up task dependency
    includeDirs.forEach { inputs.dir(it).withPathSensitivity(PathSensitivity.RELATIVE) }
}

dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" {
        kotlin.srcDir(bindingsRoot.map { it.dir("kotlin") })
    }
}

val cppApiElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
    }
}

val cppLinkElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

val cppRuntimeElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.DYNAMIC_LIB))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    selfHeaders.forEach { add(cppApiElements.name, layout.projectDirectory.dir(it)) }
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
    add(cppRuntimeElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}

val updatePrebuilt by tasks.registering(Sync::class) {
    enabled = usePrebuiltSources
    into(prebuiltRoot)
    from(generatedRoot)
}