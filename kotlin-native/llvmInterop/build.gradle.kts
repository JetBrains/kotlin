import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.solib

plugins {
    kotlin("jvm")
    id("native-interop-plugin")
    id("native")
    id("native-dependencies")
}

val library = solib("llvmstubs")

val cppLink by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_LINK))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

val cppApi by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.API))
    }
}

dependencies {
    cppApi(project(":kotlin-native:llvmDebugInfoC"))
    cppLink(project(":kotlin-native:llvmDebugInfoC"))
    cppApi(project(":kotlin-native:libllvmext"))
    cppLink(project(":kotlin-native:libllvmext"))
}

val includeFlags = listOf(
        "-I${nativeDependencies.llvmPath}/include",
        "-I${rootProject.project(":kotlin-native:llvmDebugInfoC").projectDir}/src/main/include",
        "-I${rootProject.project(":kotlin-native:libllvmext").projectDir}/src/main/include",
        *nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni
)
val commonFlags = listOf(
        "-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wmissing-field-initializers",
        "-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wdelete-non-virtual-dtor",
        "-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS",
)
val cflags = commonFlags + listOf("-std=c99")

val ldflags = listOfNotNull(
        "-L${nativeDependencies.hostPlatform.absoluteTargetSysRoot}/usr/lib".takeIf {
            // $llvmDir/lib contains libc++.1.dylib too, and it seems to be preferred by the linker
            // over the sysroot-provided one.
            // As a result, libllvmstubs.dylib gets linked with $llvmDir/lib/libc++.1.dylib.
            // It has install_name = @rpath/libc++.1.dylib, which won't work for us, because
            // dynamic loader won't be able to find libc++ when loading libllvmstubs.
            // For some reason, this worked fine before macOS 12.3.
            //
            // To enforce linking with proper libc++, pass the default path explicitly.
            PlatformInfo.isMac()
        },
        "-L${nativeDependencies.llvmPath}/lib",
        "-L${rootProject.project(":kotlin-native:llvmDebugInfoC").layout.buildDirectory.get().asFile}",
        "-L${rootProject.project(":kotlin-native:libllvmext").layout.buildDirectory.get().asFile}",
        "-fvisibility-inlines-hidden",
        "-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wcast-qual", "-Wmissing-field-initializers",
        "-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wnon-virtual-dtor", "-Wdelete-non-virtual-dtor",
        "-std=c++17",
        "-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS",
        "-ldebugInfo", "-lllvmext"
) + if (PlatformInfo.isMac()) {
    // -lLLVM* flags are produced by the following command:
    // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto objcarcopts arm aarch64 webassembly x86 mips
    listOf(
            "-Wl,-search_paths_first", "-Wl,-headerpad_max_install_names",
            "-lpthread", "-lz", "-lm", "-lcurses", "-Wl,-U,_futimens", "-Wl,-U,_LLVMDumpType",
            "-Wl,-exported_symbols_list,llvm.list",
            "-lLLVMMipsDisassembler", "-lLLVMMipsAsmParser", "-lLLVMMipsCodeGen", "-lLLVMMipsDesc", "-lLLVMMipsInfo", "-lLLVMX86TargetMCA", "-lLLVMMCA",
            "-lLLVMX86Disassembler", "-lLLVMX86AsmParser", "-lLLVMX86CodeGen", "-lLLVMX86Desc", "-lLLVMX86Info", "-lLLVMWebAssemblyDisassembler",
            "-lLLVMWebAssemblyAsmParser", "-lLLVMWebAssemblyCodeGen", "-lLLVMWebAssemblyDesc", "-lLLVMWebAssemblyUtils", "-lLLVMWebAssemblyInfo",
            "-lLLVMAArch64Disassembler", "-lLLVMAArch64AsmParser", "-lLLVMAArch64CodeGen", "-lLLVMAArch64Desc", "-lLLVMAArch64Utils", "-lLLVMAArch64Info",
            "-lLLVMARMDisassembler", "-lLLVMARMAsmParser", "-lLLVMARMCodeGen", "-lLLVMCFGuard", "-lLLVMGlobalISel", "-lLLVMSelectionDAG", "-lLLVMAsmPrinter",
            "-lLLVMARMDesc", "-lLLVMMCDisassembler", "-lLLVMARMUtils", "-lLLVMARMInfo", "-lLLVMLTO", "-lLLVMRemoteCachingService", "-lLLVMRemoteNullService",
            "-lLLVMPasses", "-lLLVMCoroutines", "-lLLVMObjCARCOpts", "-lLLVMExtensions", "-lLLVMCodeGen", "-lLLVMCAS", "-lLLVMipo", "-lLLVMInstrumentation",
            "-lLLVMVectorize", "-lLLVMFrontendOpenMP", "-lLLVMScalarOpts", "-lLLVMInstCombine", "-lLLVMAggressiveInstCombine", "-lLLVMTarget", "-lLLVMLinker",
            "-lLLVMTransformUtils", "-lLLVMBitWriter", "-lLLVMAnalysis", "-lLLVMProfileData", "-lLLVMSymbolize", "-lLLVMDebugInfoPDB", "-lLLVMDebugInfoMSF",
            "-lLLVMDebugInfoDWARF", "-lLLVMObject", "-lLLVMTextAPI", "-lLLVMMCParser", "-lLLVMIRReader", "-lLLVMAsmParser", "-lLLVMMC", "-lLLVMDebugInfoCodeView",
            "-lLLVMBitReader", "-lLLVMCore", "-lLLVMRemarks", "-lLLVMBitstreamReader", "-lLLVMBinaryFormat", "-lLLVMSupport", "-lLLVMDemangle"
    )
} else if (PlatformInfo.isLinux()) {
    // -lLLVM* flags are produced by the following command:
    // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto arm aarch64 webassembly x86 mips
    listOf(
            "-Wl,-z,noexecstack",
            "-lrt", "-ldl", "-lpthread", "-lz", "-lm",
            "-lLLVMMipsDisassembler", "-lLLVMMipsAsmParser", "-lLLVMMipsCodeGen", "-lLLVMMipsDesc", "-lLLVMMipsInfo", "-lLLVMX86TargetMCA", "-lLLVMMCA",
            "-lLLVMX86Disassembler", "-lLLVMX86AsmParser", "-lLLVMX86CodeGen", "-lLLVMX86Desc", "-lLLVMX86Info", "-lLLVMWebAssemblyDisassembler",
            "-lLLVMWebAssemblyAsmParser", "-lLLVMWebAssemblyCodeGen", "-lLLVMWebAssemblyDesc", "-lLLVMWebAssemblyUtils", "-lLLVMWebAssemblyInfo",
            "-lLLVMAArch64Disassembler", "-lLLVMAArch64AsmParser", "-lLLVMAArch64CodeGen", "-lLLVMAArch64Desc", "-lLLVMAArch64Utils", "-lLLVMAArch64Info",
            "-lLLVMARMDisassembler", "-lLLVMARMAsmParser", "-lLLVMARMCodeGen", "-lLLVMCFGuard", "-lLLVMGlobalISel", "-lLLVMSelectionDAG", "-lLLVMAsmPrinter",
            "-lLLVMARMDesc", "-lLLVMMCDisassembler", "-lLLVMARMUtils", "-lLLVMARMInfo", "-lLLVMLTO", "-lLLVMPasses", "-lLLVMIRPrinter", "-lLLVMCoroutines",
            "-lLLVMExtensions", "-lLLVMCodeGen", "-lLLVMObjCARCOpts", "-lLLVMipo", "-lLLVMInstrumentation", "-lLLVMVectorize", "-lLLVMFrontendOpenMP", "-lLLVMScalarOpts",
            "-lLLVMInstCombine", "-lLLVMAggressiveInstCombine", "-lLLVMTarget", "-lLLVMLinker", "-lLLVMTransformUtils", "-lLLVMBitWriter", "-lLLVMAnalysis",
            "-lLLVMProfileData", "-lLLVMSymbolize", "-lLLVMDebugInfoPDB", "-lLLVMDebugInfoMSF", "-lLLVMDebugInfoDWARF", "-lLLVMObject", "-lLLVMTextAPI", "-lLLVMMCParser",
            "-lLLVMIRReader", "-lLLVMAsmParser", "-lLLVMMC", "-lLLVMDebugInfoCodeView", "-lLLVMBitReader", "-lLLVMCore", "-lLLVMRemarks", "-lLLVMBitstreamReader",
            "-lLLVMBinaryFormat", "-lLLVMTargetParser", "-lLLVMSupport", "-lLLVMDemangle"
    )
} else if (PlatformInfo.isWindows()) {
    // -lLLVM* flags are produced by the following command:
    // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto arm aarch64 webassembly x86
    listOf(
            "-lLLVMX86TargetMCA", "-lLLVMMCA", "-lLLVMX86Disassembler", "-lLLVMX86AsmParser", "-lLLVMX86CodeGen", "-lLLVMX86Desc", "-lLLVMX86Info",
            "-lLLVMWebAssemblyDisassembler", "-lLLVMWebAssemblyAsmParser", "-lLLVMWebAssemblyCodeGen", "-lLLVMWebAssemblyDesc", "-lLLVMWebAssemblyUtils",
            "-lLLVMWebAssemblyInfo", "-lLLVMAArch64Disassembler", "-lLLVMAArch64AsmParser", "-lLLVMAArch64CodeGen", "-lLLVMAArch64Desc", "-lLLVMAArch64Utils",
            "-lLLVMAArch64Info", "-lLLVMARMDisassembler", "-lLLVMARMAsmParser", "-lLLVMARMCodeGen", "-lLLVMCFGuard", "-lLLVMGlobalISel", "-lLLVMSelectionDAG",
            "-lLLVMAsmPrinter", "-lLLVMARMDesc", "-lLLVMMCDisassembler", "-lLLVMARMUtils", "-lLLVMARMInfo", "-lLLVMLTO", "-lLLVMPasses", "-lLLVMIRPrinter",
            "-lLLVMCoroutines", "-lLLVMExtensions", "-lLLVMCodeGen", "-lLLVMObjCARCOpts", "-lLLVMipo", "-lLLVMInstrumentation", "-lLLVMVectorize", "-lLLVMFrontendOpenMP",
            "-lLLVMScalarOpts", "-lLLVMInstCombine", "-lLLVMAggressiveInstCombine", "-lLLVMTarget", "-lLLVMLinker", "-lLLVMTransformUtils", "-lLLVMBitWriter",
            "-lLLVMAnalysis", "-lLLVMProfileData", "-lLLVMSymbolize", "-lLLVMDebugInfoPDB", "-lLLVMDebugInfoMSF", "-lLLVMDebugInfoDWARF", "-lLLVMObject", "-lLLVMTextAPI",
            "-lLLVMMCParser", "-lLLVMIRReader", "-lLLVMAsmParser", "-lLLVMMC", "-lLLVMDebugInfoCodeView", "-lLLVMBitReader", "-lLLVMCore", "-lLLVMRemarks",
            "-lLLVMBitstreamReader", "-lLLVMBinaryFormat", "-lLLVMTargetParser", "-lLLVMSupport", "-lLLVMDemangle",
            "-lpsapi", "-lshell32", "-lole32", "-luuid", "-ladvapi32"
    )
} else {
    emptyList()
}

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags(*includeFlags.toTypedArray(), *cflags.toTypedArray(),
                    "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            file(layout.buildDirectory.file("nativeInteropStubs/llvm/c/stubs.c").get().asFile.toRelativeString(layout.projectDirectory.asFile))
        }
    }

    target(library, sourceSets["main"]!!.transform(".c" to ".$obj")) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags(
                "-shared",
                "-o", ruleOut(), *ruleInAll(),
                *ldflags.toTypedArray())
    }
}

tasks.named(library).configure {
    inputs.files(cppLink)
}

kotlinNativeInterop.create("llvm").genTask.configure {
    defFile.set(layout.projectDirectory.file("llvm.def"))
    compilerOptions.addAll(cflags)
    headersDirs.from("${nativeDependencies.llvmPath}/include")
    headersDirs.from(cppApi)
}

native.sourceSets["main"]!!.implicitTasks()
tasks.named("stubs.o").configure {
    inputs.file(kotlinNativeInterop["llvm"].genTask.map { it.cBridge })
}

dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" {
        kotlin.srcDir(kotlinNativeInterop["llvm"].genTask.map { layout.buildDirectory.dir("nativeInteropStubs/llvm/kotlin") })
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
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

val cppRuntimeElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LIBRARY_RUNTIME))
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
    add(cppRuntimeElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}