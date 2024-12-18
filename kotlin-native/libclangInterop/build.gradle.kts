import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.tools.lib

plugins {
    id("native-interop-plugin")
}

dependencies {
    implementation(kotlinStdlib()) // `kotlinStdlib()` is not available in kotlin-native/build-tools project
    cppImplementation(project(":kotlin-native:libclangext"))
    cppLink(project(":kotlin-native:libclangext"))
}

nativeInteropPlugin {
    defFileName.set("clang.def")
    usePrebuiltSources.set(true)
    commonCompilerArgs.set(emptyList<String>())
    cCompilerArgs.set(listOf("-std=c99"))
    cppCompilerArgs.set(listOf("-std=c++11"))
    selfHeaders.set(emptyList<String>())
    systemIncludeDirs.set(listOf("${nativeDependencies.llvmPath}/include"))
    linkerArgs.set(buildList {
        if (PlatformInfo.isMac()) {
            addAll(listOf("-Wl,--no-demangle", "-Wl,-search_paths_first", "-Wl,-headerpad_max_install_names"))
            // Let some symbols be undefined to avoid linking unnecessary parts.
            listOf(
                    "_futimens",
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
            ).mapTo(this) { "-Wl,-U,$it" }
            addAll(listOf("-lpthread", "-lz", "-lm", "-lcurses"))
        }
    })
    additionalLinkedStaticLibraries.set(buildList {
        val libclang = if (HostManager.hostIsMingw) {
            "lib/libclang.lib"
        } else {
            "lib/${System.mapLibraryName("clang")}"
        }
        add("${nativeDependencies.llvmPath}/$libclang")
        if (PlatformInfo.isMac()) {
            listOf(
                    "clangAST", "clangASTMatchers", "clangAnalysis", "clangBasic", "clangDriver", "clangEdit",
                    "clangFrontend", "clangFrontendTool", "clangLex", "clangParse", "clangSema",
                    "clangRewrite", "clangRewriteFrontend", "clangStaticAnalyzerFrontend",
                    "clangStaticAnalyzerCheckers", "clangStaticAnalyzerCore", "clangSerialization",
                    "clangToolingCore",
                    "clangTooling", "clangFormat", "LLVMTarget", "LLVMMC", "LLVMLinker", "LLVMTransformUtils",
                    "LLVMBitWriter", "LLVMBitReader", "LLVMAnalysis", "LLVMProfileData", "LLVMCore",
                    "LLVMSupport", "LLVMBinaryFormat", "LLVMDemangle"
            ).mapTo(this) { "${nativeDependencies.llvmPath}/lib/${lib(it)}" }
        }
    })
}

projectTest(jUnitMode = JUnitMode.JUnit5) // `projectTest()` is not available in kotlin-native/build-tools project