import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetWithSanitizer
import org.jetbrains.kotlin.tools.ToolExecutionTask
import org.jetbrains.kotlin.tools.libname
import org.jetbrains.kotlin.tools.obj
import org.jetbrains.kotlin.tools.solib

plugins {
    kotlin("jvm")
    id("native-interop-plugin")
    id("native")
}

val stubsName = "llvmstubs"
val library = solib(stubsName)
val objFile = obj(stubsName)
val cFile = "$stubsName.c"

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
    cppImplementation(project(":kotlin-native:llvmDebugInfoC"))
    cppLink(project(":kotlin-native:llvmDebugInfoC"))
    cppImplementation(project(":kotlin-native:libllvmext"))
    cppLink(project(":kotlin-native:libllvmext"))
}

val cflags = buildList {
    add("-std=c99")
    addAll(listOf("-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wmissing-field-initializers"))
    addAll(listOf("-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wdelete-non-virtual-dtor"))
    addAll(listOf("-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS"))
    add("-I${nativeDependencies.llvmPath}/include")
    addAll(cppImplementation.files.map { "-I${it.absolutePath}" })
    addAll(nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni)
}

val ldflags = buildList {
    add("-fvisibility-inlines-hidden")
    addAll(listOf("-Wall", "-W", "-Wno-unused-parameter", "-Wwrite-strings", "-Wcast-qual", "-Wmissing-field-initializers"))
    addAll(listOf("-pedantic", "-Wno-long-long", "-Wcovered-switch-default", "-Wnon-virtual-dtor", "-Wdelete-non-virtual-dtor"))
    add("-std=c++17")
    addAll(listOf("-DNDEBUG", "-D__STDC_CONSTANT_MACROS", "-D__STDC_FORMAT_MACROS", "-D__STDC_LIMIT_MACROS"))
    addAll(cppLink.files.flatMap {
        listOf("-L${it.parentFile.absolutePath}", "-l${libname(it)}")
    })

    if (PlatformInfo.isMac()) {
        // -lLLVM* flags are produced by the following command:
        // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto objcarcopts arm aarch64 webassembly x86 mips
        addAll(listOf("-Wl,-search_paths_first", "-Wl,-headerpad_max_install_names"))
        addAll(listOf("-lpthread", "-lz", "-lm", "-lcurses", "-Wl,-U,_futimens", "-Wl,-U,_LLVMDumpType"))
        add("-Wl,-exported_symbols_list,llvm.list")
        addAll(listOf("-lLLVMMipsDisassembler", "-lLLVMMipsAsmParser", "-lLLVMMipsCodeGen", "-lLLVMMipsDesc", "-lLLVMMipsInfo", "-lLLVMX86TargetMCA", "-lLLVMMCA"))
        addAll(listOf("-lLLVMX86Disassembler", "-lLLVMX86AsmParser", "-lLLVMX86CodeGen", "-lLLVMX86Desc", "-lLLVMX86Info", "-lLLVMWebAssemblyDisassembler"))
        addAll(listOf("-lLLVMWebAssemblyAsmParser", "-lLLVMWebAssemblyCodeGen", "-lLLVMWebAssemblyDesc", "-lLLVMWebAssemblyUtils", "-lLLVMWebAssemblyInfo"))
        addAll(listOf("-lLLVMAArch64Disassembler", "-lLLVMAArch64AsmParser", "-lLLVMAArch64CodeGen", "-lLLVMAArch64Desc", "-lLLVMAArch64Utils", "-lLLVMAArch64Info"))
        addAll(listOf("-lLLVMARMDisassembler", "-lLLVMARMAsmParser", "-lLLVMARMCodeGen", "-lLLVMCFGuard", "-lLLVMGlobalISel", "-lLLVMSelectionDAG", "-lLLVMAsmPrinter"))
        addAll(listOf("-lLLVMARMDesc", "-lLLVMMCDisassembler", "-lLLVMARMUtils", "-lLLVMARMInfo", "-lLLVMLTO", "-lLLVMRemoteCachingService", "-lLLVMRemoteNullService"))
        addAll(listOf("-lLLVMPasses", "-lLLVMCoroutines", "-lLLVMObjCARCOpts", "-lLLVMExtensions", "-lLLVMCodeGen", "-lLLVMCAS", "-lLLVMipo", "-lLLVMInstrumentation"))
        addAll(listOf("-lLLVMVectorize", "-lLLVMFrontendOpenMP", "-lLLVMScalarOpts", "-lLLVMInstCombine", "-lLLVMAggressiveInstCombine", "-lLLVMTarget", "-lLLVMLinker"))
        addAll(listOf("-lLLVMTransformUtils", "-lLLVMBitWriter", "-lLLVMAnalysis", "-lLLVMProfileData", "-lLLVMSymbolize", "-lLLVMDebugInfoPDB", "-lLLVMDebugInfoMSF"))
        addAll(listOf("-lLLVMDebugInfoDWARF", "-lLLVMObject", "-lLLVMTextAPI", "-lLLVMMCParser", "-lLLVMIRReader", "-lLLVMAsmParser", "-lLLVMMC", "-lLLVMDebugInfoCodeView"))
        addAll(listOf("-lLLVMBitReader", "-lLLVMCore", "-lLLVMRemarks", "-lLLVMBitstreamReader", "-lLLVMBinaryFormat", "-lLLVMSupport", "-lLLVMDemangle"))
    } else if (PlatformInfo.isLinux()) {
        // -lLLVM* flags are produced by the following command:
        // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto arm aarch64 webassembly x86 mips
        add("-Wl,-z,noexecstack")
        addAll(listOf("-lrt", "-ldl", "-lpthread", "-lz", "-lm"))
        addAll(listOf("-lLLVMMipsDisassembler", "-lLLVMMipsAsmParser", "-lLLVMMipsCodeGen", "-lLLVMMipsDesc", "-lLLVMMipsInfo", "-lLLVMX86TargetMCA", "-lLLVMMCA"))
        addAll(listOf("-lLLVMX86Disassembler", "-lLLVMX86AsmParser", "-lLLVMX86CodeGen", "-lLLVMX86Desc", "-lLLVMX86Info", "-lLLVMWebAssemblyDisassembler"))
        addAll(listOf("-lLLVMWebAssemblyAsmParser", "-lLLVMWebAssemblyCodeGen", "-lLLVMWebAssemblyDesc", "-lLLVMWebAssemblyUtils", "-lLLVMWebAssemblyInfo"))
        addAll(listOf("-lLLVMAArch64Disassembler", "-lLLVMAArch64AsmParser", "-lLLVMAArch64CodeGen", "-lLLVMAArch64Desc", "-lLLVMAArch64Utils", "-lLLVMAArch64Info"))
        addAll(listOf("-lLLVMARMDisassembler", "-lLLVMARMAsmParser", "-lLLVMARMCodeGen", "-lLLVMCFGuard", "-lLLVMGlobalISel", "-lLLVMSelectionDAG", "-lLLVMAsmPrinter"))
        addAll(listOf("-lLLVMARMDesc", "-lLLVMMCDisassembler", "-lLLVMARMUtils", "-lLLVMARMInfo", "-lLLVMLTO", "-lLLVMPasses", "-lLLVMIRPrinter", "-lLLVMCoroutines"))
        addAll(listOf("-lLLVMExtensions", "-lLLVMCodeGen", "-lLLVMObjCARCOpts", "-lLLVMipo", "-lLLVMInstrumentation", "-lLLVMVectorize", "-lLLVMFrontendOpenMP", "-lLLVMScalarOpts"))
        addAll(listOf("-lLLVMInstCombine", "-lLLVMAggressiveInstCombine", "-lLLVMTarget", "-lLLVMLinker", "-lLLVMTransformUtils", "-lLLVMBitWriter", "-lLLVMAnalysis"))
        addAll(listOf("-lLLVMProfileData", "-lLLVMSymbolize", "-lLLVMDebugInfoPDB", "-lLLVMDebugInfoMSF", "-lLLVMDebugInfoDWARF", "-lLLVMObject", "-lLLVMTextAPI", "-lLLVMMCParser"))
        addAll(listOf("-lLLVMIRReader", "-lLLVMAsmParser", "-lLLVMMC", "-lLLVMDebugInfoCodeView", "-lLLVMBitReader", "-lLLVMCore", "-lLLVMRemarks", "-lLLVMBitstreamReader"))
        addAll(listOf("-lLLVMBinaryFormat", "-lLLVMTargetParser", "-lLLVMSupport", "-lLLVMDemangle"))
    } else if (PlatformInfo.isWindows()) {
        // -lLLVM* flags are produced by the following command:
        // ./llvm-config --libs analysis bitreader bitwriter core linker target analysis ipo instrumentation lto arm aarch64 webassembly x86
        addAll(listOf("-lLLVMX86TargetMCA", "-lLLVMMCA", "-lLLVMX86Disassembler", "-lLLVMX86AsmParser", "-lLLVMX86CodeGen", "-lLLVMX86Desc", "-lLLVMX86Info"))
        addAll(listOf("-lLLVMWebAssemblyDisassembler", "-lLLVMWebAssemblyAsmParser", "-lLLVMWebAssemblyCodeGen", "-lLLVMWebAssemblyDesc", "-lLLVMWebAssemblyUtils"))
        addAll(listOf("-lLLVMWebAssemblyInfo", "-lLLVMAArch64Disassembler", "-lLLVMAArch64AsmParser", "-lLLVMAArch64CodeGen", "-lLLVMAArch64Desc", "-lLLVMAArch64Utils"))
        addAll(listOf("-lLLVMAArch64Info", "-lLLVMARMDisassembler", "-lLLVMARMAsmParser", "-lLLVMARMCodeGen", "-lLLVMCFGuard", "-lLLVMGlobalISel", "-lLLVMSelectionDAG"))
        addAll(listOf("-lLLVMAsmPrinter", "-lLLVMARMDesc", "-lLLVMMCDisassembler", "-lLLVMARMUtils", "-lLLVMARMInfo", "-lLLVMLTO", "-lLLVMPasses", "-lLLVMIRPrinter"))
        addAll(listOf("-lLLVMCoroutines", "-lLLVMExtensions", "-lLLVMCodeGen", "-lLLVMObjCARCOpts", "-lLLVMipo", "-lLLVMInstrumentation", "-lLLVMVectorize", "-lLLVMFrontendOpenMP"))
        addAll(listOf("-lLLVMScalarOpts", "-lLLVMInstCombine", "-lLLVMAggressiveInstCombine", "-lLLVMTarget", "-lLLVMLinker", "-lLLVMTransformUtils", "-lLLVMBitWriter"))
        addAll(listOf("-lLLVMAnalysis", "-lLLVMProfileData", "-lLLVMSymbolize", "-lLLVMDebugInfoPDB", "-lLLVMDebugInfoMSF", "-lLLVMDebugInfoDWARF", "-lLLVMObject", "-lLLVMTextAPI"))
        addAll(listOf("-lLLVMMCParser", "-lLLVMIRReader", "-lLLVMAsmParser", "-lLLVMMC", "-lLLVMDebugInfoCodeView", "-lLLVMBitReader", "-lLLVMCore", "-lLLVMRemarks"))
        addAll(listOf("-lLLVMBitstreamReader", "-lLLVMBinaryFormat", "-lLLVMTargetParser", "-lLLVMSupport", "-lLLVMDemangle"))
        addAll(listOf("-lpsapi", "-lshell32", "-lole32", "-luuid", "-ladvapi32"))
    }

    if (PlatformInfo.isMac()) {
        // $llvmDir/lib contains libc++.1.dylib too, and it seems to be preferred by the linker
        // over the sysroot-provided one.
        // As a result, libllvmstubs.dylib gets linked with $llvmDir/lib/libc++.1.dylib.
        // It has install_name = @rpath/libc++.1.dylib, which won't work for us, because
        // dynamic loader won't be able to find libc++ when loading libllvmstubs.
        // For some reason, this worked fine before macOS 12.3.
        //
        // To enforce linking with proper libc++, pass the default path explicitly:
        add("-L${nativeDependencies.hostPlatform.absoluteTargetSysRoot}/usr/lib")
    }
    add("-L${nativeDependencies.llvmPath}/lib")
}

native {
    val obj = if (HostManager.hostIsMingw) "obj" else "o"
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags(*cflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
        }
    }
    sourceSet {
        "main" {
            file(layout.buildDirectory.file("interopTemp/$cFile").get().asFile.toRelativeString(layout.projectDirectory.asFile))
        }
    }

    target(library, sourceSets["main"]!!.transform(".c" to ".$obj")) {
        tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
        flags("-shared", "-o", ruleOut(), *ruleInAll(), *ldflags.toTypedArray())
    }
}

kotlinNativeInterop {
    create("llvm") {
        defFile("llvm.def")
        compilerOpts(cflags)
        skipNatives()
        genTask.configure {
            cppImplementation.files.forEach { inputs.dir(it) }
        }
    }
}

native.sourceSets["main"]!!.implicitTasks()
tasks.named(library).configure {
    inputs.files(cppLink)
}
tasks.named(objFile).configure {
    inputs.file(kotlinNativeInterop["llvm"].genTask.map { layout.buildDirectory.file("interopTemp/$cFile") })
    cppImplementation.files.forEach { inputs.dir(it) }
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
    add(cppLinkElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
    add(cppRuntimeElements.name, tasks.named<ToolExecutionTask>(library).map { it.output })
}