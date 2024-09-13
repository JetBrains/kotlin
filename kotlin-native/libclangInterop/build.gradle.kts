import org.gradle.kotlin.dsl.named
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

val libclangextProject = project(":kotlin-native:libclangext")
val libclangextDir = libclangextProject.layout.buildDirectory.get().asFile
val libclangextIsEnabled = libclangextProject.findProperty("isEnabled")!! as Boolean

val library = solib("clangstubs")

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
    cppApi(project(":kotlin-native:libclangext"))
    cppLink(project(":kotlin-native:libclangext"))
}

val libclang =
        if (HostManager.hostIsMingw) {
            "lib/libclang.lib"
        } else {
            "lib/${System.mapLibraryName("clang")}"
        }

val includeFlags = listOf("-I${nativeDependencies.llvmPath}/include",
        "-I${project(":kotlin-native:libclangext").projectDir.absolutePath}/src/main/include",
        *nativeDependencies.hostPlatform.clangForJni.hostCompilerArgsForJni)
val cflags = listOf("-std=c99")
val cxxflags = listOf("-std=c++11")

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
    suffixes {
        (".c" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangC("").toTypedArray())
            flags(*includeFlags.toTypedArray(), *cflags.toTypedArray(),
                    "-c", "-o", ruleOut(), ruleInFirst())
        }
        (".cpp" to ".$obj") {
            tool(*hostPlatform.clangForJni.clangCXX("").toTypedArray())
            flags(*includeFlags.toTypedArray(), *cxxflags.toTypedArray(), "-c", "-o", ruleOut(), ruleInFirst())
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

    target(library, *objSet) {
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

kotlinNativeInterop.create("clang").genTask.configure {
    defFile.set(layout.projectDirectory.file("clang.def"))
    compilerOptions.addAll(cflags)
    headersDirs.from("${nativeDependencies.llvmPath}/include")
    headersDirs.from(cppApi)
}

dependencies {
    implementation(kotlinStdlib())
    api(project(":kotlin-native:Interop:Runtime"))
}

sourceSets {
    "main" {
        kotlin.srcDir("prebuilt/nativeInteropStubs/kotlin")
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

// TODO: Replace with a common way to generate sources. Also add a test that generation didn't change checked-in sources.
val updatePrebuilt by tasks.registering(Sync::class) {
    into(layout.projectDirectory.dir("prebuilt/nativeInteropStubs"))

    from(kotlinNativeInterop["clang"].genTask.map { it.kotlinBridges }) {
        include("clang/clang.kt")
        into("kotlin")
    }

    from(kotlinNativeInterop["clang"].genTask.map { it.cBridge }) {
        into("c")
        rename("stubs.c", "clangstubs.c")
    }
}