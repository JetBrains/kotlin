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

val defFileName = "clang.def"
val usePrebuiltSources = true
val implementationDependencies = listOf(
        ":kotlin-native:libclangext",
)
val commonCompilerArgs = emptyList<String>()
val cCompilerArgs = listOf("-std=c99")
val cppCompilerArgs = listOf("-std=c++11")
val selfHeaders = emptyList<String>()
val systemIncludeDirs = listOf("${nativeDependencies.llvmPath}/include")
val linkerArgs = buildList {
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
}
val additionalLinkedStaticLibraries = buildList {
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