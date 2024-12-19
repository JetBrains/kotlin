import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.tools.lib

plugins {
    id("native-interop-plugin")
}

dependencies {
    implementation(kotlinStdlib()) // `kotlinStdlib()` is not available in kotlin-native/build-tools project
    cppImplementation(project(":kotlin-native:llvmDebugInfoC"))
    cppLink(project(":kotlin-native:llvmDebugInfoC"))
    cppImplementation(project(":kotlin-native:libllvmext"))
    cppLink(project(":kotlin-native:libllvmext"))
}

nativeInteropPlugin {
    defFileName.set("llvm.def")
    usePrebuiltSources.set(true)
    commonCompilerArgs.set(buildList {
        addAll(listOf("-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wmissing-field-initializers"))
        addAll(listOf("-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wdelete-non-virtual-dtor"))
        addAll(listOf("-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS"))
    })
    cCompilerArgs.set(listOf("-std=c99"))
    cppCompilerArgs.set(listOf("-std=c++11"))
    selfHeaders.set(emptyList<String>())
    systemIncludeDirs.set(listOf("${nativeDependencies.llvmPath}/include"))
    linkerArgs.set(buildList {
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
    })
    additionalLinkedStaticLibraries.set(buildList {
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
    })
}

projectTest(jUnitMode = JUnitMode.JUnit5) // `projectTest()` is not available in kotlin-native/build-tools project