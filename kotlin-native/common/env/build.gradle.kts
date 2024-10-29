import org.gradle.kotlin.dsl.named
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

val defFileName = "env.konan.backend.kotlin.jetbrains.org.def"
val usePrebuiltSources = false
val implementationDependencies = emptyList<String>()
val commonCompilerArgs = listOf("-Wall", "-O2")
val cCompilerArgs = listOf("-std=c99")
val cppCompilerArgs = listOf("-std=c++11")
val selfHeaders = listOf("src/main/headers")
val systemIncludeDirs = emptyList<String>()
val linkerArgs = emptyList<String>()
val additionalLinkedStaticLibraries = emptyList<String>()

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
        defFile(defFileName)
        val cflags = cCompilerArgs + commonCompilerArgs + includeDirs.map { "-I${it.absolutePath}" }
        compilerOpts(cflags)
        genTask.configure {
            includeDirs.forEach { inputs.dir(it).withPathSensitivity(PathSensitivity.RELATIVE) }
        }
    }
}

val prebuiltRoot = layout.projectDirectory.dir("gen/main")
val generatedRoot = kotlinNativeInterop["main"].genTask.map { layout.buildDirectory.dir("nativeInteropStubs/main").get() }

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